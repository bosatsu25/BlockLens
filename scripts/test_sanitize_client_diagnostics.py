"""Privacy and bounds tests for the failure-only client log summary."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).with_name("sanitize-client-diagnostics.py")
SPEC = importlib.util.spec_from_file_location("client_diagnostics", SCRIPT)
diagnostics = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(diagnostics)

# Controlled CI #336 probe values, without raw log messages or identifiers.
ANALYZER_SEEDED = (
    "BLOCKLENS_ANALYZER_RETURN stage=SEEDED markerMask=0 configMask=31 loadedMask=31 "
    "physicalMask=31 beaconBaseMask=511 overworld=true sameLevel=true sameVillager=true "
    "trackedVillager=true alive=true entityVisible=true memoryPresent=true memoryDimension=true "
    "memoryExpectedSite=true eyeAnchor=true villagerInRange=true siteInRange=true "
    "entitySectionAccessible=true entityInSection=true flying=true cells=512 sections=13 "
    "steps=571 entitySections=4 entities=1"
)
ANALYZER_TIMEOUT = (
    "BLOCKLENS_ANALYZER_RETURN stage=TIMEOUT markerMask=15 configMask=31 loadedMask=31 "
    "physicalMask=31 beaconBaseMask=511 overworld=true sameLevel=true sameVillager=false "
    "trackedVillager=true alive=true entityVisible=true memoryPresent=false memoryDimension=false "
    "memoryExpectedSite=false eyeAnchor=true villagerInRange=true siteInRange=true "
    "entitySectionAccessible=true entityInSection=true flying=true cells=512 sections=13 "
    "steps=731 entitySections=4 entities=2"
)
ANALYZER_LOG_PREFIX = "[12:09:57] [Render thread/INFO] (Minecraft) [STDOUT]: "
ANALYZER_FILE_PREFIX = "[12:09:57] [Render thread/INFO]: [STDOUT]: "


def replace_probe_field(record, key, value):
    return " ".join(key + "=" + str(value) if token.startswith(key + "=") else token
                    for token in record.split(" "))


class ClientDiagnosticsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.log = self.root / "versions/mc26_3/build/run/clientGameTest/logs/latest.log"
        self.log.parent.mkdir(parents=True)

    def summary(self, data, module="mc26_3"):
        self.log.write_bytes(data if isinstance(data, bytes) else data.encode("utf-8"))
        return diagnostics.summarize(self.root, module)

    def test_useful_types_and_allowlisted_frames_survive_without_messages(self):
        result = self.summary(
            '[00:00:00] [Render thread/ERROR]: java.lang.NoClassDefFoundError: PRIVATE_MESSAGE\n'
            'Caused by: org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException: PRIVATE_CAUSE\n'
            '\tat java.base/java.lang.Thread.run(Thread.java:123)\n'
            '\tat fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld(SchematicWorldHandler.java:1)\n'
            '\tat dev.blocklens.fabric.BuilderClient.tick(BuilderClient.java:2)\n'
            '\tat private.account.Secret.read(Private.java:3)\n'
        )
        self.assertIn("available=true\n", result)
        self.assertIn("exceptionType=java.lang.NoClassDefFoundError\n", result)
        self.assertIn("exceptionType=org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException\n", result)
        self.assertIn("frame=java.lang.Thread.run\n", result)
        self.assertIn("frame=fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld\n", result)
        self.assertIn("frame=dev.blocklens.fabric.BuilderClient.tick\n", result)
        for private in ("PRIVATE_MESSAGE", "PRIVATE_CAUSE", "private.account", "Thread.java", ":123"):
            self.assertNotIn(private, result)

    def test_messages_locations_and_thread_names_are_never_emitted(self):
        private = "secret-test-user C:\\private\\do-not-export 203.0.113.77 https://invalid.example/token 12345678-1234-1234-1234-123456789abc NBT{secret} chat=secret argv=--secret"
        result = self.summary(
            'Exception in thread "' + private + '" java.lang.IllegalStateException: ' + private + '\n'
            'Suppressed: java.io.IOException: ' + private + '\n'
            '\tat net.minecraft.client.Minecraft.run(' + private + ')\n'
            '\tat org.lwjgl.opengl.GL.create(' + private + ')\n'
        )
        self.assertIn("exceptionType=java.lang.IllegalStateException\n", result)
        self.assertIn("exceptionType=java.io.IOException\n", result)
        self.assertIn("frame=net.minecraft.client.Minecraft.run\n", result)
        for token in private.split():
            self.assertNotIn(token, result)
        self.assertNotIn("(", result)
        self.assertNotIn(")", result)

    def test_ordinary_chat_urls_world_data_and_argv_are_ignored(self):
        result = self.summary(
            "chat: java.lang.RuntimeException: not an exception header\n"
            "world={private} inventory={private} --token=private\n"
            "https://invalid.example/java.lang.AssertionError\n"
            "at https://invalid.example/net.minecraft.client.Minecraft.run(private)\n"
            "at C:\\private\\net.minecraft.client.Minecraft.run(private)\n"
        )
        self.assertIn("available=true\n", result)
        self.assertNotIn("exceptionType=", result)
        self.assertNotIn("frame=", result)
        self.assertNotIn("private", result)

    def test_unlisted_exception_uses_only_a_fixed_flag(self):
        result = self.summary("Caused by: private.account.SecretException: secret\n")
        self.assertIn("unlistedException=true\n", result)
        self.assertNotIn("private", result)
        self.assertNotIn("Secret", result)

    def test_only_exact_namespace_prefixes_are_accepted(self):
        result = self.summary(
            "at net.minecraftEvil.private.Secret.method(secret)\n"
            "at fi.dy.masaEvil.private.Secret.method(secret)\n"
            "at dev.blocklensEvil.private.Secret.method(secret)\n"
            "at org.lwjglEvil.private.Secret.method(secret)\n"
            "at javax.swing.SwingUtilities.invokeLater(SwingUtilities.java:1)\n"
            "at jdk.internal.misc.Unsafe.park(Native Method)\n"
        )
        self.assertIn("frame=javax.swing.SwingUtilities.invokeLater\n", result)
        self.assertIn("frame=jdk.internal.misc.Unsafe.park\n", result)
        self.assertNotIn("Evil", result)
        self.assertNotIn("Secret", result)

    def test_loader_module_frames_and_constructors_keep_only_public_symbols(self):
        result = self.summary(
            "at knot//net.minecraft.client.Minecraft.<init>(private-location)\n"
            "at app/java.base/java.lang.Thread.run(private-location)\n"
            "at java.base@25/java.lang.Thread.start(private-location)\n"
        )
        self.assertIn("frame=net.minecraft.client.Minecraft.<init>\n", result)
        self.assertIn("frame=java.lang.Thread.run\n", result)
        self.assertIn("frame=java.lang.Thread.start\n", result)
        self.assertNotIn("knot", result)
        self.assertNotIn("app/", result)
        self.assertNotIn("@25", result)
        self.assertNotIn("private", result)

    def test_opaque_identifier_symbols_are_rejected(self):
        result = self.summary(
            "at dev.blocklens.private.Uuid_12345678_1234_1234_1234_123456789abc.read(private)\n"
            "java.lang.Secret12345678123412341234123456789abcException: private\n"
        )
        self.assertNotIn("frame=", result)
        self.assertNotIn("exceptionType=", result)
        self.assertNotIn("12345678", result)

    def test_missing_log_always_emits_fixed_unavailable_flags(self):
        result = diagnostics.summarize(self.root, "mc26_3")
        self.assertIn("available=false\n", result)
        self.assertIn("inputRejected=false\n", result)
        self.assertNotIn(str(self.root), result)

    def test_directory_log_is_rejected(self):
        self.log.mkdir()
        result = diagnostics.summarize(self.root, "mc26_3")
        self.assertIn("available=false\n", result)
        self.assertIn("inputRejected=true\n", result)

    def test_invalid_modules_never_read_or_echo_the_input(self):
        self.log.write_text("java.lang.AssertionError: secret\n", encoding="utf-8")
        for module in ("../mc26_3", "/private/log", "mc26_3/../../secret", "mc26_4", "--token=secret"):
            with self.subTest(module=module):
                result = diagnostics.summarize(self.root, module)
                self.assertIn("inputRejected=true\n", result)
                self.assertNotIn("exceptionType=", result)
                self.assertNotIn(module, result)

    def make_symlink(self, source, target, directory=False):
        try:
            source.symlink_to(target, target_is_directory=directory)
        except (OSError, NotImplementedError):
            self.skipTest("Symlink creation is unavailable on this test host")

    def test_symlink_log_is_rejected(self):
        elsewhere = self.root / "elsewhere.log"
        elsewhere.write_text("java.lang.AssertionError: secret\n", encoding="utf-8")
        self.make_symlink(self.log, elsewhere)
        self.assertIn("inputRejected=true\n", diagnostics.summarize(self.root, "mc26_3"))

    def test_symlink_parent_is_rejected(self):
        outside = self.root / "other"
        outside.mkdir()
        self.log.parent.rmdir()
        self.make_symlink(self.log.parent, outside, directory=True)
        (outside / "latest.log").write_text("java.lang.AssertionError: secret\n", encoding="utf-8")
        self.assertIn("inputRejected=true\n", diagnostics.summarize(self.root, "mc26_3"))

    def test_symlink_repo_is_rejected(self):
        self.log.write_text("java.lang.AssertionError: secret\n", encoding="utf-8")
        link = self.root / "linked-root"
        self.make_symlink(link, self.root, directory=True)
        self.assertIn("inputRejected=true\n", diagnostics.summarize(link, "mc26_3"))

    def test_byte_budget_does_not_select_tail_data(self):
        head = b"java.lang.IllegalStateException: secret\n"
        result = self.summary(head + b"x" * (diagnostics.MAX_BYTES - len(head))
                              + b"\njava.lang.AssertionError: tail-secret\n")
        self.assertIn("byteTruncated=true\n", result)
        self.assertIn("exceptionType=java.lang.IllegalStateException\n", result)
        self.assertNotIn("AssertionError", result)
        self.assertNotIn("secret", result)

    def test_line_budget_does_not_select_tail_data(self):
        result = self.summary("java.lang.IllegalStateException: secret\n"
                              + "ordinary\n" * (diagnostics.MAX_LINES - 1)
                              + "java.lang.AssertionError: secret\n")
        self.assertIn("lineTruncated=true\n", result)
        self.assertNotIn("AssertionError", result)

    def test_exact_line_budget_and_crlf_are_not_truncated(self):
        result = self.summary("java.lang.IllegalStateException: secret\r\n"
                              + "ordinary\r\n" * (diagnostics.MAX_LINES - 1))
        self.assertIn("lineTruncated=false\n", result)
        self.assertIn("exceptionType=java.lang.IllegalStateException\n", result)

    def test_selected_line_budget_is_bounded(self):
        with patch.object(diagnostics, "MAX_SELECTED", 2):
            result = self.summary("".join(
                "at %s(F.java:1)\n" % symbol for symbol in sorted(diagnostics.FRAME_SYMBOLS)[:3]))
        frames = [line for line in result.splitlines() if line.startswith("frame=")]
        self.assertEqual(2, len(frames))
        self.assertIn("selectedTruncated=true\n", result)

    def test_forged_multiline_chat_symbols_never_reach_public_output(self):
        result = self.summary('[00:00:00] [Render thread/INFO]: [CHAT] first line\n'
                              'at java.lang.PRIVATE_ACCOUNT_TEST.render(Unknown Source)\n'
                              'java.lang.PRIVATE_ACCOUNT_TESTException: private\n'
                              'at dev.blocklens.private.UserName.method(Unknown Source)\n')
        self.assertIn("unlistedException=true\n", result)
        self.assertIn("unlistedFrame=true\n", result)
        self.assertNotIn("PRIVATE_ACCOUNT_TEST", result)
        self.assertNotIn("UserName", result)
        self.assertNotIn("frame=", result)
        self.assertNotIn("exceptionType=", result)

    def test_duplicate_symbols_do_not_consume_selected_budget(self):
        result = self.summary("at net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:1)\n" * 256)
        self.assertEqual(1, result.count("frame="))
        self.assertIn("selectedTruncated=false\n", result)

    def test_ci336_failure_retains_bounded_return_diagnosis_and_failing_oracle(self):
        result = self.summary(
            ANALYZER_LOG_PREFIX + ANALYZER_SEEDED + "\n"
            + ANALYZER_FILE_PREFIX + ANALYZER_TIMEOUT + "\n"
            + "java.lang.AssertionError: PRIVATE_TIMEOUT_MESSAGE\n"
            + "\tat knot//net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl.waitFor(ClientGameTestContextImpl.java:199)\n"
            + "\tat knot//dev.blocklens.gametest.AnalyzerVisualOracle.verify(AnalyzerVisualOracle.java:115)\n"
            + "\tat knot//dev.blocklens.gametest.BlockLensSmokeClientGameTest.runTest(BlockLensSmokeClientGameTest.java:57)\n"
        )
        self.assertIn(ANALYZER_SEEDED + "\n", result)
        self.assertIn(ANALYZER_TIMEOUT + "\n", result)
        self.assertIn("exceptionType=java.lang.AssertionError\n", result)
        self.assertIn("frame=dev.blocklens.gametest.AnalyzerVisualOracle.verify\n", result)
        self.assertIn("frame=net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl.waitFor\n", result)
        for private in ("PRIVATE_TIMEOUT_MESSAGE", "Render thread", "12:09:57", ".java:", "knot//"):
            self.assertNotIn(private, result)

    def test_reviewed_visual_oracle_frames_survive_without_arbitrary_methods(self):
        for symbol in ("dev.blocklens.gametest.AnalyzerVisualOracle.verify",
                       "dev.blocklens.gametest.SceneFilterVisualOracle.verify",
                       "dev.blocklens.gametest.VillagerJobSiteFixture.verifyReplacement",
                       "dev.blocklens.gametest.VillagerJobSiteFixture.require",
                       "net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl.waitFor"):
            with self.subTest(symbol=symbol):
                result = self.summary("at knot//" + symbol + "(private-location)\n"
                                      + "at knot//" + symbol + "Private(private-location)\n")
                self.assertIn("frame=" + symbol + "\n", result)
                self.assertIn("unlistedFrame=true\n", result)
                self.assertNotIn("Private", result)
                self.assertNotIn("private-location", result)

    def test_return_records_are_reconstructed_in_fixed_order(self):
        tokens = ANALYZER_SEEDED.split(" ")
        reordered = tokens[0] + " " + " ".join(reversed(tokens[1:]))
        result = self.summary(ANALYZER_LOG_PREFIX + reordered + "\n")
        self.assertIn(ANALYZER_SEEDED + "\n", result)
        self.assertNotIn(reordered, result)

    def test_return_counters_accept_zero_and_their_reviewed_maxima(self):
        bounds = {"markerMask": 31, "configMask": 31, "loadedMask": 31, "physicalMask": 31,
                  "beaconBaseMask": 511, "cells": 512, "sections": 64, "steps": 4096,
                  "entitySections": 125, "entities": 64}
        for key, maximum in bounds.items():
            for value in (0, maximum):
                with self.subTest(key=key, value=value):
                    record = replace_probe_field(ANALYZER_SEEDED, key, value)
                    self.assertIn(record + "\n", self.summary(ANALYZER_LOG_PREFIX + record + "\n"))

    def test_return_counters_reject_overflow_and_noncanonical_numbers(self):
        bounds = {"markerMask": 31, "configMask": 31, "loadedMask": 31, "physicalMask": 31,
                  "beaconBaseMask": 511, "cells": 512, "sections": 64, "steps": 4096,
                  "entitySections": 125, "entities": 64}
        for key, maximum in bounds.items():
            for value in (-1, maximum + 1, "+1", "01", "1.0", "1e0", "\u0661", "private"):
                with self.subTest(key=key, value=value):
                    record = replace_probe_field(ANALYZER_SEEDED, key, value)
                    result = self.summary(ANALYZER_LOG_PREFIX + record + "\n")
                    self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
                    self.assertNotIn("private", result)

    def test_return_booleans_accept_only_exact_true_and_false(self):
        keys = [token.split("=")[0] for token in ANALYZER_SEEDED.split(" ")[1:]
                if token.endswith("=true")]
        for key in keys:
            record = replace_probe_field(ANALYZER_SEEDED, key, "false")
            with self.subTest(key=key, value="false"):
                self.assertIn(record + "\n", self.summary(ANALYZER_FILE_PREFIX + record + "\n"))
            for value in ("True", "FALSE", "0", "1", "private", ""):
                with self.subTest(key=key, value=value):
                    record = replace_probe_field(ANALYZER_SEEDED, key, value)
                    result = self.summary(ANALYZER_LOG_PREFIX + record + "\n")
                    self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
                    self.assertNotIn("private", result)

    def test_return_records_reject_unknown_missing_duplicate_and_malformed_fields(self):
        variants = [ANALYZER_SEEDED + " extra=private",
                    ANALYZER_SEEDED + " entities=1",
                    ANALYZER_SEEDED.replace(" entities=1", ""),
                    ANALYZER_SEEDED.replace("entities=1", "unknown=1"),
                    ANALYZER_SEEDED.replace("entities=1", "entities==1"),
                    ANALYZER_SEEDED.replace("entities=1", "entities"),
                    ANALYZER_SEEDED.replace(" cells=512", "\tcells=512"),
                    ANALYZER_SEEDED + "\x00",
                    ANALYZER_SEEDED + " private"]
        variants.extend(replace_probe_field(ANALYZER_SEEDED, "stage", stage)
                        for stage in ("BEFORE", "seeded", "TIMEOUT_PRIVATE", ""))
        for record in variants:
            with self.subTest(record=record):
                result = self.summary(ANALYZER_LOG_PREFIX + record + "\n")
                self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
                self.assertNotIn("private", result)

    def test_chat_and_other_prefixes_cannot_become_return_records(self):
        for prefix in ("", "[CHAT] ", "chat: ", "[12:09:57] [Render thread/INFO]: [CHAT] ",
                       "[12:09:57] [Render thread/INFO] (Minecraft) [STDOUT]: [CHAT] ",
                       "[12:09:57] [private/INFO]: [STDOUT]: ",
                       "[12:09:57] [Render thread/INFO] (MinecraftPrivate) [STDOUT]: ",
                       "https://invalid.example/", "java.lang.RuntimeException: "):
            with self.subTest(prefix=prefix):
                result = self.summary(prefix + ANALYZER_SEEDED + "\n")
                self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
                self.assertNotIn("private", result)

    def test_return_records_never_export_identifiers_or_free_text(self):
        for private in ("C:\\private\\file", "203.0.113.77", "https://invalid.example/token",
                        "12345678-1234-1234-1234-123456789abc", "PRIVATE_ACCOUNT", "NBT{private}"):
            for record in (replace_probe_field(ANALYZER_SEEDED, "entities", private),
                           replace_probe_field(ANALYZER_SEEDED, "sameVillager", private),
                           ANALYZER_SEEDED + " " + private + "=true"):
                with self.subTest(private=private, record=record):
                    result = self.summary(ANALYZER_LOG_PREFIX + record + "\n")
                    self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
                    self.assertNotIn(private, result)

    def test_return_records_share_selection_budget_and_duplicate_suppression(self):
        with patch.object(diagnostics, "MAX_SELECTED", 1):
            duplicate = (ANALYZER_LOG_PREFIX + ANALYZER_SEEDED + "\n") * 2
            result = self.summary(duplicate)
            self.assertEqual(1, result.count("BLOCKLENS_ANALYZER_RETURN"))
            self.assertIn("selectedTruncated=false\n", result)
            result = self.summary(duplicate + ANALYZER_LOG_PREFIX + ANALYZER_TIMEOUT + "\n")
            self.assertEqual(1, result.count("BLOCKLENS_ANALYZER_RETURN"))
            self.assertIn("selectedTruncated=true\n", result)
            self.assertNotIn("stage=TIMEOUT", result)

    def test_return_records_share_input_byte_line_and_length_budgets(self):
        record = ANALYZER_LOG_PREFIX + ANALYZER_SEEDED + "\n"
        with patch.object(diagnostics, "MAX_LINES", 1):
            result = self.summary("ordinary\n" + record)
            self.assertIn("lineTruncated=true\n", result)
            self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
        with patch.object(diagnostics, "MAX_BYTES", len(record) - 3):
            result = self.summary(record)
            self.assertIn("byteTruncated=true\n", result)
            self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
        with patch.object(diagnostics, "MAX_LINE_LENGTH", len(record) - 3):
            result = self.summary(record)
            self.assertIn("lineLengthTruncated=true\n", result)
            self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)

    def test_byte_cutoff_cannot_hide_extra_fields_after_an_apparently_complete_record(self):
        record = ANALYZER_LOG_PREFIX + ANALYZER_SEEDED
        with patch.object(diagnostics, "MAX_BYTES", len(record)):
            result = self.summary(record + " extra=private\n")
            self.assertIn("byteTruncated=true\n", result)
            self.assertNotIn("BLOCKLENS_ANALYZER_RETURN", result)
            self.assertNotIn("private", result)

    def test_invalid_utf8_and_overlong_line_cannot_leak_or_block_later_type(self):
        result = self.summary(b"\xffprivate\n" + b"x" * 4096
                              + b"\njava.lang.IllegalStateException: secret\n")
        self.assertIn("lineLengthTruncated=true\n", result)
        self.assertIn("exceptionType=java.lang.IllegalStateException\n", result)
        self.assertNotIn("private", result)
        self.assertNotIn("secret", result)

    def test_cli_rejects_untrusted_arguments_without_echo_or_traceback(self):
        result = subprocess.run([sys.executable, str(SCRIPT), "../secret?token=private"],
                                capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode)
        self.assertIn("available=false\n", result.stdout)
        self.assertIn("inputRejected=true\n", result.stdout)
        self.assertEqual("", result.stderr)
        self.assertNotIn("private", result.stdout)
        self.assertNotIn("Traceback", result.stdout)


if __name__ == "__main__":
    unittest.main()
