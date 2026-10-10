"""Emit bounded public diagnostic fields, never raw client log text."""
import os
from pathlib import Path
import re
import stat
import sys

MAX_BYTES = 1024 * 1024
MAX_LINES = 4096
MAX_SELECTED = 128
MAX_LINE_LENGTH = 2048
MAX_SYMBOL_LENGTH = 256
MODULES = frozenset(("mc26_1_2", "mc26_2", "mc26_3"))
EXCEPTION_TYPES = frozenset((
    "java.lang.AssertionError", "java.lang.IllegalArgumentException", "java.lang.IllegalStateException",
    "java.lang.RuntimeException", "java.lang.NullPointerException", "java.lang.NoClassDefFoundError",
    "java.lang.ClassNotFoundException", "java.lang.OutOfMemoryError", "java.lang.LinkageError",
    "java.io.IOException", "java.util.concurrent.CompletionException",
    "java.util.concurrent.ExecutionException", "java.util.concurrent.TimeoutException",
    "org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException",
))
FRAME_SYMBOLS = frozenset((
    "java.lang.Thread.run", "java.lang.Thread.start", "javax.swing.SwingUtilities.invokeLater",
    "jdk.internal.misc.Unsafe.park", "net.minecraft.client.Minecraft.run",
    "net.minecraft.client.Minecraft.<init>",
    "net.fabricmc.loader.impl.FabricLoaderImpl.load",
    "fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld",
    "dev.blocklens.fabric.BuilderClient.tick",
    "dev.blocklens.gametest.ResponsiveSettingsScreenOracle.require",
    "dev.blocklens.gametest.ResponsiveSettingsScreenOracle.verifySize",
    "dev.blocklens.gametest.BlockLensSmokeClientGameTest.runTest",
    "dev.blocklens.gametest.M8PerformanceBaselineOracle.verify",
    "dev.blocklens.gametest.AnalyzerVisualOracle.verify",
    "dev.blocklens.gametest.SceneFilterVisualOracle.verify",
    "dev.blocklens.gametest.VillagerJobSiteFixture.verifyReplacement",
    "dev.blocklens.gametest.VillagerJobSiteFixture.require",
    "net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl.waitFor",
))
BOOLEAN_VALUES = frozenset(("true", "false"))
ANALYZER_MARKER = "BLOCKLENS_ANALYZER_RETURN"
# Fixed field order and accepted values from the test-only AnalyzerReturnProbe.
ANALYZER_FIELDS = {
    "stage": frozenset(("SEEDED", "TIMEOUT")),
    "markerMask": 31, "configMask": 31, "loadedMask": 31, "physicalMask": 31,
    "beaconBaseMask": 511,
    "overworld": BOOLEAN_VALUES, "sameLevel": BOOLEAN_VALUES, "sameVillager": BOOLEAN_VALUES,
    "trackedVillager": BOOLEAN_VALUES, "alive": BOOLEAN_VALUES, "entityVisible": BOOLEAN_VALUES,
    "memoryPresent": BOOLEAN_VALUES, "memoryDimension": BOOLEAN_VALUES,
    "memoryExpectedSite": BOOLEAN_VALUES, "eyeAnchor": BOOLEAN_VALUES,
    "villagerInRange": BOOLEAN_VALUES, "siteInRange": BOOLEAN_VALUES,
    "entitySectionAccessible": BOOLEAN_VALUES, "entityInSection": BOOLEAN_VALUES,
    "flying": BOOLEAN_VALUES,
    "cells": 512, "sections": 64, "steps": 4096, "entitySections": 125, "entities": 64,
}
# Native console and latest.log STDOUT envelopes only; generic prefix stripping
# would also remove [CHAT], allowing it to masquerade as a controlled probe.
ANALYZER_PREFIX = re.compile(r"^\[(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]\] "
                             r"\[Render thread/INFO\](?: \(Minecraft\) |: )\[STDOUT\]: ")
COUNTER_VALUE = re.compile(r"(?:0|[1-9][0-9]{0,3})")
FLAGS = ("available", "inputRejected", "readFailed", "byteTruncated", "lineTruncated",
         "lineLengthTruncated", "selectedTruncated", "unlistedException", "unlistedFrame")
IDENTIFIER = r"[A-Za-z_$][A-Za-z0-9_$]{0,95}"
QUALIFIED = IDENTIFIER + r"(?:\." + IDENTIFIER + r"){1,15}"
PREFIX = re.compile(r"^(?:\[[^\]\r\n]{1,256}\]\s*){1,4}:?\s*")
FRAME = re.compile(r"^at\s+(?:[A-Za-z0-9_.@-]{1,96}/(?:[A-Za-z0-9_.@-]{1,96}/|/)?)?("
                   + QUALIFIED + r")\.(" + IDENTIFIER
                   + r"|<init>|<clinit>)\([^\r\n]*\)\s*$")
EXCEPTION = re.compile(r'^(?:(?:Caused by:|Suppressed:)\s*|Exception in thread "[^"\r\n]{0,512}"\s*)?('
                       + QUALIFIED + r")(?=[:\s]|$)")
OPAQUE_ID = re.compile(r"[0-9a-fA-F]{32}|[0-9a-fA-F]{8}[-_][0-9a-fA-F]{4}[-_]"
                      r"[0-9a-fA-F]{4}[-_][0-9a-fA-F]{4}[-_][0-9a-fA-F]{12}")


class RejectedInput(Exception):
    """An input alias or nonregular path was rejected without exposing its value."""


def _aliased(info):
    return stat.S_ISLNK(info.st_mode) or bool(
        getattr(info, "st_file_attributes", 0) & getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400))


def _checked_path(repo, module):
    root = Path(repo).absolute()
    if root != root.resolve(strict=True):
        raise RejectedInput()
    info = root.lstat()
    if _aliased(info) or not stat.S_ISDIR(info.st_mode):
        raise RejectedInput()
    path = root
    parts = ("versions", module, "build", "run", "clientGameTest", "logs", "latest.log")
    for number, part in enumerate(parts):
        path = path / part
        info = path.lstat()
        expected = stat.S_ISREG if number == len(parts) - 1 else stat.S_ISDIR
        if _aliased(info) or not expected(info.st_mode):
            raise RejectedInput()
    if not path.resolve(strict=True).is_relative_to(root):
        raise RejectedInput()
    return path, info


def _render(flags, selected=()):
    return "".join(name + "=" + ("true" if flags.get(name, False) else "false") + "\n"
                   for name in FLAGS) + "".join(line + "\n" for line in selected)


def _analyzer_return(line):
    prefix = ANALYZER_PREFIX.match(line)
    if prefix is None:
        return None
    tokens = line[prefix.end():].split(" ")
    if len(tokens) != len(ANALYZER_FIELDS) + 1 or tokens[0] != ANALYZER_MARKER:
        return None
    values = {}
    for token in tokens[1:]:
        key, separator, value = token.partition("=")
        if not separator or key not in ANALYZER_FIELDS or key in values:
            return None
        accepted = ANALYZER_FIELDS[key]
        if isinstance(accepted, int):
            if COUNTER_VALUE.fullmatch(value) is None or int(value) > accepted:
                return None
            values[key] = str(int(value))
        else:
            if value not in accepted:
                return None
            values[key] = value
    # Reconstruct from reviewed keys and validated values, never copy raw text.
    return ANALYZER_MARKER + " " + " ".join(key + "=" + values[key] for key in ANALYZER_FIELDS)


def summarize(repo, module):
    flags = {}
    if not isinstance(module, str) or module not in MODULES:
        return _render({"inputRejected": True})
    try:
        path, original = _checked_path(repo, module)
        open_flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0) | getattr(os, "O_BINARY", 0)
        descriptor = os.open(path, open_flags)
        with os.fdopen(descriptor, "rb") as source:
            opened = os.fstat(source.fileno())
            if not stat.S_ISREG(opened.st_mode) or (opened.st_dev, opened.st_ino) != (original.st_dev, original.st_ino):
                raise RejectedInput()
            data = source.read(MAX_BYTES)
        _, after = _checked_path(repo, module)
        if (after.st_dev, after.st_ino) != (opened.st_dev, opened.st_ino):
            raise RejectedInput()
        flags["available"] = True
        flags["byteTruncated"] = max(opened.st_size, after.st_size) > MAX_BYTES
    except FileNotFoundError:
        return _render(flags)
    except (RejectedInput, ValueError, TypeError, RuntimeError):
        return _render({"inputRejected": True})
    except OSError:
        return _render({"readFailed": True})

    # Limit splitting as well as parsing; CR and CRLF cannot evade the line budget.
    lines = data.replace(b"\r\n", b"\n").replace(b"\r", b"\n").split(b"\n", MAX_LINES)
    flags["lineTruncated"] = len(lines) > MAX_LINES and bool(lines[-1])
    selected, seen = [], set()
    for number, raw in enumerate(lines[:MAX_LINES]):
        if len(raw) > MAX_LINE_LENGTH:
            flags["lineLengthTruncated"] = True
            continue
        decoded = raw.decode("utf-8", errors="replace")
        partial_tail = (flags["byteTruncated"] and number == len(lines) - 1
                        and not data.endswith((b"\n", b"\r")))
        # A byte cutoff can hide an extra field or the remaining counter digits.
        symbol = None if partial_tail else _analyzer_return(decoded)
        # A complete probe is longer than a method symbol, but remains bounded by
        # the input line limit, fixed field count and each field's value domain.
        selected_limit = MAX_LINE_LENGTH if symbol is not None else MAX_SYMBOL_LENGTH
        if symbol is None:
            line = PREFIX.sub("", decoded.strip(), count=1)
            match = FRAME.fullmatch(line)
            if match:
                public_class, method = match.groups()
                if public_class + "." + method in FRAME_SYMBOLS:
                    symbol = "frame=" + public_class + "." + method
                else:
                    flags["unlistedFrame"] = True
            else:
                match = EXCEPTION.match(line)
                if match and match.group(1).endswith(("Exception", "Error", "Throwable")):
                    public_type = match.group(1)
                    if public_type in EXCEPTION_TYPES:
                        symbol = "exceptionType=" + public_type
                    else:
                        flags["unlistedException"] = True
        if symbol is None or len(symbol) > selected_limit or OPAQUE_ID.search(symbol):
            continue
        if symbol in seen:
            continue
        if len(selected) == MAX_SELECTED:
            flags["selectedTruncated"] = True
            continue
        seen.add(symbol)
        selected.append(symbol)
    return _render(flags, selected)


def main(argv):
    if len(argv) != 1:
        sys.stdout.write(_render({"inputRejected": True}))
    else:
        sys.stdout.write(summarize(Path(__file__).absolute().parent.parent, argv[0]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
