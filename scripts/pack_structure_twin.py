#!/usr/bin/env python3
"""Structural twin of the source resource pack for the pack-vs-mod cost comparison.

``extract`` reads a resource-pack ZIP and writes a structure profile that keeps only what changes
load, bake and mesh cost: entry order and kind, vanilla blockstate names and state keys, the model
reference graph, element/face counts with cullface/shade/AO flags, image dimensions, PNG color type,
byte sizes and animation parameters. Pixels, element coordinates, UVs, credits, text and the names of
resources the pack adds in its own folders are not recorded. Names directly inside a category folder
(``textures/block/obsidian.png``) are kept: such a file replaces the vanilla resource of that name,
and replacing versus adding decides atlas growth. ``generate`` rebuilds a loadable pack from that profile with
synthetic pixels and anonymized resource names. ``signature`` and ``compare`` report how closely a
generated twin matches the recorded source structure.

The profile is derived data; the source archive itself never enters the repository or CI.
"""
import argparse
import gzip
import hashlib
import io
import json
import random
import re
import struct
import sys
import zipfile
import zlib
from pathlib import Path

FORMAT = "blocklens-pack-structure/1"
TWIN = "bltwin"
# Directories whose names Minecraft or the pack layout fixes; every other name is anonymized.
KEPT_DIR = re.compile(r"^assets/(minecraft/((blockstates|models|textures|lang|items)/((block|item)/)?)?)?$")
NAMED_FILE = re.compile(r"^assets/minecraft/(blockstates|models|textures|lang)/(.+)$")
# Free text and coordinates are replaced; structural numbers listed here are kept.
BLANKED_STRINGS = {"credit", "name", "__comment", "format_version"}
KEPT_NUMBERS = {"x", "y", "tintindex", "frametime", "frames", "angle", "index", "time", "weight",
                "light_emission", "width", "height", "rotation"}
CANONICAL_LISTS = {"from": [0, 0, 0], "to": [16, 16, 16], "uv": [0, 0, 16, 16], "origin": [8, 8, 8],
                   "translation": [0, 0, 0], "scale": [1, 1, 1], "rotation": [0, 0, 0]}
# Privacy guard for every string the profile records.
SAFE_STRING = re.compile(r"^[a-z0-9_:/#@.=,|!-]*$")
TOLERANCE = 0.10


def kind_of(name):
    if name.endswith("/"):
        return "d"
    if name.endswith(".rpo"):
        return "r"
    if name == "pack.mcmeta":
        return "p"
    found = NAMED_FILE.match(name)
    if found:
        folder, rest = found.groups()
        if folder == "blockstates" and rest.endswith(".json") and "/" not in rest:
            return "b"
        if folder == "models" and rest.endswith(".json"):
            return "m"
        if folder == "textures" and rest.endswith(".png"):
            return "t"
        if folder == "textures" and rest.endswith(".png.mcmeta"):
            return "a"
        if folder == "lang" and rest.endswith(".json"):
            return "l"
    return "o"


def category(name):
    """models/<category>/... or textures/<category>/...; the category decides atlas and model set."""
    return NAMED_FILE.match(name).group(2).split("/")[0]


def flat(name):
    """A file directly in its category folder, where vanilla keeps every resource of that kind."""
    return NAMED_FILE.match(name).group(2).count("/") == 1


def png_layout(data):
    """(width, height, color type, bit depth, IDAT bytes, other bytes, PLTE bytes, tRNS bytes)."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    offset, idat, chunks, plte, trns, header = 8, 0, 0, 0, 0, None
    while offset < len(data):
        length, tag = struct.unpack(">I4s", data[offset:offset + 8])
        body = data[offset + 8:offset + 8 + length]
        if tag == b"IHDR":
            header = struct.unpack(">IIBB", body[:10])
        elif tag == b"IDAT":
            idat += length
            chunks += 1
        elif tag == b"PLTE":
            plte = length
        elif tag == b"tRNS":
            trns = length
        offset += 12 + length
    width, height, depth, color = header
    fixed = 8 + 25 + 12 + idat + 12 * max(chunks, 1)
    fixed += (12 + plte if plte else 0) + (12 + trns if trns else 0)
    return [width, height, color, depth, idat, len(data) - fixed, plte, trns]


class Extractor:
    def __init__(self, names):
        self.models = {}
        self.textures = {}
        for name in names:
            kind = kind_of(name)
            if kind == "m" and not flat(name):
                self.models[self.identifier(name, "models", ".json")] = len(self.models)
            elif kind == "t" and not flat(name):
                self.textures[self.identifier(name, "textures", ".png")] = len(self.textures)

    def place(self, record, name, folder, suffix, table):
        """Flat files keep their (vanilla) name; files in the pack's own folders get an index."""
        record["c"] = category(name)
        if flat(name):
            record["n"] = name.split("/")[-1][:-len(suffix)]
        else:
            record["i"] = table[self.identifier(name, folder, suffix)]

    @staticmethod
    def identifier(name, folder, suffix):
        return "minecraft:" + name[len(f"assets/minecraft/{folder}/"):-len(suffix)]

    def reference(self, value, table, mark):
        if value.startswith("#"):
            return value
        full = value if ":" in value else "minecraft:" + value
        return f"@{mark}{table[full]}" if full in table else full

    def canonical(self, value, key=None, parent=None):
        if isinstance(value, dict):
            result = {}
            for name, item in value.items():
                if name in ("parent", "model") and isinstance(item, str):
                    result[name] = self.reference(item, self.models, "m")
                elif (name == "texture" or key == "textures") and isinstance(item, str):
                    result[name] = self.reference(item, self.textures, "t")
                elif name in BLANKED_STRINGS and isinstance(item, str):
                    result[name] = ""
                else:
                    result[name] = self.canonical(item, name, key)
            return result
        if isinstance(value, list):
            if key in CANONICAL_LISTS and all(isinstance(item, (int, float)) for item in value):
                return list(CANONICAL_LISTS[key][:len(value)])
            return [self.canonical(item, key, parent) for item in value]
        if isinstance(value, bool) or isinstance(value, str) or value is None:
            return value
        return value if key in KEPT_NUMBERS else 0


def extract(pack):
    data = Path(pack).read_bytes()
    archive = zipfile.ZipFile(io.BytesIO(data))
    infos = archive.infolist()
    extractor = Extractor([info.filename for info in infos])
    records = []
    for info in infos:
        name, kind = info.filename, kind_of(info.filename)
        record = {"k": kind, "z": info.compress_type}
        body = b"" if kind == "d" else archive.read(info)
        if kind == "d":
            record["p"] = name if KEPT_DIR.match(name) else None
        elif kind == "b":
            record["n"] = name.split("/")[-1][:-5]
            record["j"] = extractor.canonical(json.loads(body.decode("utf-8-sig")))
        elif kind == "m":
            extractor.place(record, name, "models", ".json", extractor.models)
            record["j"] = extractor.canonical(json.loads(body.decode("utf-8-sig")))
        elif kind == "t":
            extractor.place(record, name, "textures", ".png", extractor.textures)
            record["png"] = png_layout(body)
        elif kind == "a":
            extractor.place(record, name[:-7], "textures", ".png", extractor.textures)
            record["j"] = extractor.canonical(json.loads(body.decode("utf-8-sig")))
        elif kind == "l":
            record["n"] = name.split("/")[-1][:-5]
            record["e"] = len(json.loads(body.decode("utf-8-sig")))
            record["s"] = len(body)
        elif kind == "p":
            pack_meta = json.loads(body.decode("utf-8-sig"))
            pack_meta["pack"]["description"] = ""
            record["j"] = pack_meta
        elif kind == "r":
            target = name[:-4]
            target_kind = kind_of(target)
            if target_kind == "b":
                record["t"] = "b:" + target.split("/")[-1][:-5]
            elif target_kind in ("m", "t") and flat(target):
                record["t"] = "f:" + target
            elif target_kind == "m":
                record["t"] = "m:%d" % extractor.models[extractor.identifier(target, "models", ".json")]
            elif target_kind == "t":
                record["t"] = "t:%d" % extractor.textures[extractor.identifier(target, "textures", ".png")]
            else:
                record["t"] = "o"
            try:
                record["j"] = json.loads(body.decode("utf-8-sig"))
            except ValueError:
                record["s"] = len(body)
        else:
            record["x"] = name.rsplit(".", 1)[-1].lower() if "." in name.split("/")[-1] else ""
            if name == "pack.png" or record["x"] == "png":
                record["png"] = png_layout(body)
                record["p"] = name if name == "pack.png" else None
            else:
                record["s"] = len(body)
        records.append(record)
    header = {"format": FORMAT, "sha256": hashlib.sha256(data).hexdigest(), "signature": signature_of(archive)}
    return header, records


def unsafe_strings(value, path=""):
    """Every recorded string that could carry names or text; the committed profile has none."""
    found = []
    if isinstance(value, dict):
        for name, item in value.items():
            found += unsafe_strings(name, path) + unsafe_strings(item, path + "/" + name)
    elif isinstance(value, list):
        for item in value:
            found += unsafe_strings(item, path)
    elif isinstance(value, str) and not SAFE_STRING.match(value):
        found.append((path, value))
    return found


def write_profile(header, records, out):
    lines = [json.dumps(header, sort_keys=True, separators=(",", ":"))]
    lines += [json.dumps(record, sort_keys=True, separators=(",", ":")) for record in records]
    text = ("\n".join(lines) + "\n").encode("utf-8")
    with open(out, "wb") as stream:
        # mtime=0 keeps the committed bytes reproducible.
        with gzip.GzipFile(fileobj=stream, mode="wb", compresslevel=9, mtime=0, filename="") as packed:
            packed.write(text)


def read_profile(path):
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        lines = [json.loads(line) for line in stream if line.strip()]
    if lines[0].get("format") != FORMAT:
        raise ValueError("unknown pack structure profile format")
    return lines[0], lines[1:]


class Generator:
    def __init__(self, records):
        self.model_category = {r["i"]: r["c"] for r in records if r["k"] == "m" and "i" in r}
        self.texture_category = {r["i"]: r["c"] for r in records if r["k"] == "t" and "i" in r}

    def model_id(self, index):
        return f"minecraft:{self.model_category[index]}/{TWIN}/m{index}"

    def texture_id(self, index):
        return f"minecraft:{self.texture_category[index]}/{TWIN}/t{index}"

    def resolve(self, value):
        if isinstance(value, dict):
            return {name: self.resolve(item) for name, item in value.items()}
        if isinstance(value, list):
            return [self.resolve(item) for item in value]
        if isinstance(value, str) and value.startswith("@m"):
            return self.model_id(int(value[2:]))
        if isinstance(value, str) and value.startswith("@t"):
            return self.texture_id(int(value[2:]))
        return value

    def path(self, record, ordinal):
        kind = record["k"]
        if kind == "b":
            return f"assets/minecraft/blockstates/{record['n']}.json"
        if kind == "m":
            leaf = record["n"] if "n" in record else f"{TWIN}/m{record['i']}"
            return f"assets/minecraft/models/{record['c']}/{leaf}.json"
        if kind in ("t", "a"):
            leaf = record["n"] if "n" in record else f"{TWIN}/t{record['i']}"
            return f"assets/minecraft/textures/{record['c']}/{leaf}.png" + (".mcmeta" if kind == "a" else "")
        if kind == "l":
            return f"assets/minecraft/lang/{record['n']}.json"
        if kind == "p":
            return "pack.mcmeta"
        if kind == "d":
            return record["p"] or f"assets/minecraft/{TWIN}/d{ordinal}/"
        if kind == "r":
            target_kind, _, key = record["t"].partition(":")
            if target_kind == "b":
                return f"assets/minecraft/blockstates/{key}.json.rpo"
            if target_kind == "f":
                return key + ".rpo"
            if target_kind in "mt" and key:
                index = int(key)
                fake = {"k": target_kind, "i": index,
                        "c": (self.model_category if target_kind == "m" else self.texture_category)[index]}
                return self.path(fake, ordinal) + ".rpo"
            return f"{TWIN}/o{ordinal}.rpo"
        if record.get("p"):
            return record["p"]
        suffix = "." + record["x"] if record.get("x") else ""
        return f"{TWIN}/o{ordinal}{suffix}"


def json_bytes(value):
    return (json.dumps(value, indent="\t", ensure_ascii=False) + "\n").encode("utf-8")


def png_chunk(tag, body):
    return struct.pack(">I", len(body)) + tag + body + struct.pack(">I", zlib.crc32(tag + body))


def synthetic_png(layout, seed):
    """A valid PNG with the recorded geometry, color type and approximately the recorded sizes."""
    width, height, color, depth, idat_target, other, plte, trns = layout
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    row_bytes = (width * channels * depth + 7) // 8
    total = row_bytes * height
    rng = random.Random(seed)
    if color == 3:
        # Palette indices must stay below the palette size; sub-byte depths pack several per byte.
        limit = min(max(1, plte // 3), 1 << depth)
        noise = bytearray()
        for _ in range(total):
            value = 0
            for _ in range(8 // depth):
                value = (value << depth) | rng.randrange(limit)
            noise.append(value)
    else:
        noise = bytearray(rng.randbytes(total))

    def raw(count):
        # A noise prefix of `count` bytes over a flat remainder tunes the compressed size.
        pixels = bytes(noise[:count]) + bytes(total - count)
        rows = b"".join(b"\x00" + pixels[y * row_bytes:(y + 1) * row_bytes] for y in range(height))
        return zlib.compress(rows, 9)

    low, high = 0, row_bytes * height
    best = raw(high)
    while low < high:
        middle = (low + high) // 2
        candidate = raw(middle)
        if abs(len(candidate) - idat_target) < abs(len(best) - idat_target):
            best = candidate
        if len(candidate) < idat_target:
            low = middle + 1
        else:
            high = middle
    output = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, depth, color, 0, 0, 0))
    if plte:
        output += png_chunk(b"PLTE", bytes(rng.randrange(256) for _ in range(plte)))
    if trns:
        output += png_chunk(b"tRNS", bytes(255 for _ in range(trns)))
    if other >= 12:
        # Private ancillary chunk standing in for the source's metadata chunks (decoders skip it).
        output += png_chunk(b"blTw", bytes(other - 12))
    return output + png_chunk(b"IDAT", best) + png_chunk(b"IEND", b"")


def generate(header, records, out):
    generator = Generator(records)
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for ordinal, record in enumerate(records):
            kind, path = record["k"], generator.path(record, ordinal)
            if kind == "d":
                body = b""
            elif kind in ("b", "m", "a", "p"):
                value = generator.resolve(record["j"])
                if kind == "p":
                    value["pack"]["description"] = "BlockLens structural twin (no source content)"
                body = json_bytes(value)
            elif kind == "t" or record.get("png"):
                body = synthetic_png(record["png"], ordinal)
            elif kind == "l":
                entries = max(record["e"], 0)
                width = max(1, (record["s"] - 4 - entries * 14) // max(entries, 1))
                body = json_bytes({f"{TWIN}.k{n}": "x" * width for n in range(entries)})
            elif kind == "r" and "j" in record:
                body = json_bytes(record["j"])
            else:
                body = b"x" * record.get("s", 0)
            info = zipfile.ZipInfo(path, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = record.get("z", zipfile.ZIP_DEFLATED)
            info.external_attr = (0o40755 << 16 | 0x10) if kind == "d" else 0o644 << 16
            archive.writestr(info, body)
    Path(out).write_bytes(buffer.getvalue())


def signature_of(archive):
    """Load-relevant structure of a pack; equal for a source pack and its generated twin."""
    names = [info.filename for info in archive.infolist()]
    sizes = {info.filename: info.file_size for info in archive.infolist()}
    s = {key: 0 for key in (
        "entries", "directories", "blockstates", "variantEntries", "variantRotations", "uvlockEntries",
        "multipartFiles", "multipartParts", "models", "itemModels", "modelsWithParent", "packParents",
        "modelsWithElements", "elements", "rotatedElements", "faces", "cullfaces", "tintedFaces",
        "aoDisabledModels", "unshadedElements", "textureRefs", "packTextureRefs", "textures",
        "blockTextures", "flatTextures", "flatModels", "texturePixels", "animated", "animationFrames",
        "palettedTextures",
        "rpoSidecars", "langEntries", "otherFiles", "jsonBytes", "pngBytes", "pngIdatBytes", "otherBytes")}
    s["dimensions"] = {}
    model_ids = {"minecraft:" + n[len("assets/minecraft/models/"):-5] for n in names if kind_of(n) == "m"}
    texture_ids = {"minecraft:" + n[len("assets/minecraft/textures/"):-4] for n in names if kind_of(n) == "t"}
    for info in archive.infolist():
        name, kind = info.filename, kind_of(info.filename)
        s["entries"] += 1
        if kind == "d":
            s["directories"] += 1
            continue
        body = archive.read(info)
        if kind in ("b", "m", "a", "l", "p"):
            s["jsonBytes"] += len(body)
        if kind == "b":
            state = json.loads(body.decode("utf-8-sig"))
            s["blockstates"] += 1
            for variant in state.get("variants", {}).values():
                for entry in variant if isinstance(variant, list) else [variant]:
                    s["variantEntries"] += 1
                    s["variantRotations"] += int(bool(entry.get("x") or entry.get("y")))
                    s["uvlockEntries"] += int(bool(entry.get("uvlock")))
            if "multipart" in state:
                s["multipartFiles"] += 1
                s["multipartParts"] += len(state["multipart"])
        elif kind == "m":
            model = json.loads(body.decode("utf-8-sig"))
            s["models"] += 1
            s["itemModels"] += int(category(name) == "item")
            s["flatModels"] += int(flat(name))
            parent = model.get("parent")
            if parent:
                s["modelsWithParent"] += 1
                s["packParents"] += int((parent if ":" in parent else "minecraft:" + parent) in model_ids)
            s["aoDisabledModels"] += int(model.get("ambientocclusion") is False)
            for value in model.get("textures", {}).values():
                if isinstance(value, str) and not value.startswith("#"):
                    s["textureRefs"] += 1
                    s["packTextureRefs"] += int((value if ":" in value else "minecraft:" + value) in texture_ids)
            elements = model.get("elements")
            if elements:
                s["modelsWithElements"] += 1
                for element in elements:
                    s["elements"] += 1
                    s["rotatedElements"] += int(bool((element.get("rotation") or {}).get("angle")))
                    s["unshadedElements"] += int(element.get("shade") is False)
                    for face in element.get("faces", {}).values():
                        s["faces"] += 1
                        s["cullfaces"] += int("cullface" in face)
                        s["tintedFaces"] += int("tintindex" in face)
        elif kind == "t" or (kind == "o" and name.endswith(".png")):
            layout = png_layout(body)
            s["pngBytes"] += len(body)
            s["pngIdatBytes"] += layout[4]
            if kind == "t":
                s["textures"] += 1
                s["blockTextures"] += int(category(name) == "block")
                s["flatTextures"] += int(flat(name))
                s["texturePixels"] += layout[0] * layout[1]
                s["palettedTextures"] += int(layout[2] == 3)
                key = f"{layout[0]}x{layout[1]}"
                s["dimensions"][key] = s["dimensions"].get(key, 0) + 1
            else:
                s["otherFiles"] += 1
        elif kind == "a":
            animation = json.loads(body.decode("utf-8-sig")).get("animation", {})
            s["animated"] += 1
            s["animationFrames"] += len(animation.get("frames", []))
        elif kind == "r":
            s["rpoSidecars"] += 1
            s["otherBytes"] += len(body)
        elif kind == "l":
            s["langEntries"] += len(json.loads(body.decode("utf-8-sig")))
        else:
            s["otherFiles"] += 1
            s["otherBytes"] += len(body)
    s["dimensions"] = dict(sorted(s["dimensions"].items()))
    return s


# Byte totals depend on synthetic content and JSON formatting; they are compared with a tolerance.
APPROXIMATE = ("jsonBytes", "pngBytes", "pngIdatBytes", "otherBytes")


def compare(source, twin):
    """Rows of (field, source, twin, ok); every structural field must match exactly."""
    rows = []
    for field, expected in source.items():
        actual = twin.get(field)
        if field in APPROXIMATE:
            ok = expected == 0 and actual == 0 or expected > 0 and abs(actual - expected) <= TOLERANCE * expected
        else:
            ok = actual == expected
        rows.append((field, expected, actual, ok))
    return rows


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest="command", required=True)
    extract_command = commands.add_parser("extract", help="write a structure profile from a pack ZIP")
    extract_command.add_argument("--pack", required=True)
    extract_command.add_argument("--profile", required=True)
    extract_command.add_argument("--sha256", help="refuse any archive with different bytes")
    generate_command = commands.add_parser("generate", help="build the twin pack ZIP from a profile")
    generate_command.add_argument("--profile", required=True)
    generate_command.add_argument("--out", required=True)
    compare_command = commands.add_parser("compare", help="compare a twin ZIP with the recorded source")
    compare_command.add_argument("--profile", required=True)
    compare_command.add_argument("--pack", required=True)
    compare_command.add_argument("--report")
    args = parser.parse_args(argv)
    if args.command == "extract":
        header, records = extract(args.pack)
        if args.sha256 and header["sha256"] != args.sha256:
            print("pack bytes do not match the pinned source archive", file=sys.stderr)
            return 1
        leaks = unsafe_strings(records)
        if leaks:
            print(f"refusing to write a profile with free text: {leaks[:3]}", file=sys.stderr)
            return 1
        write_profile(header, records, args.profile)
        return 0
    header, records = read_profile(args.profile)
    if args.command == "generate":
        generate(header, records, args.out)
        return 0
    with zipfile.ZipFile(args.pack) as archive:
        rows = compare(header["signature"], signature_of(archive))
    lines = ["| field | source | twin | match |", "| --- | ---: | ---: | --- |"]
    lines += [f"| {field} | {expected} | {actual} | {'yes' if ok else 'NO'} |" for field, expected, actual, ok in rows]
    report = "\n".join(lines) + "\n"
    if args.report:
        Path(args.report).write_text(report, encoding="utf-8")
    print(report, end="")
    return 0 if all(ok for *_, ok in rows) else 1


if __name__ == "__main__":
    sys.exit(main())
