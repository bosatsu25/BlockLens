import hashlib
import unittest
from prepare_bbe_compatibility import selected_file, validated


class BbePreparationTest(unittest.TestCase):
    def setUp(self):
        self.data = b"controlled fixture, not a third-party binary"
        self.record = {"size": len(self.data), "sha512": hashlib.sha512(self.data).hexdigest(),
                       "sha256": hashlib.sha256(self.data).hexdigest()}
        self.file = {"size": len(self.data), "hashes": {"sha512": self.record["sha512"]},
                     "url": "https://cdn.modrinth.com/data/fixture.jar"}

    def test_exact_bytes_are_required(self):
        self.assertEqual(self.data, validated(self.data, self.record))
        for data in (self.data + b"extra", b"wrong", b"x" * len(self.data)):
            with self.assertRaises(ValueError):
                validated(data, self.record)

    def test_secondary_digest_is_checked(self):
        with self.assertRaises(ValueError):
            validated(self.data, dict(self.record, sha256="0" * 64))

    def test_metadata_selects_the_inspected_file_not_a_same_version_neighbor(self):
        self.assertEqual(self.file["url"], selected_file({"files": [self.file]}, self.record))
        for files in ([], [self.file, self.file], [dict(self.file, hashes={"sha512": "0" * 128})],
                      [dict(self.file, size=1)]):
            with self.assertRaises(ValueError):
                selected_file({"files": files}, self.record)

    def test_nonofficial_urls_are_rejected(self):
        for url in ("http://cdn.modrinth.com/a", "https://example.invalid/a", "file:///tmp/a", "https://cdn.modrinth.com.evil.invalid/a"):
            with self.assertRaises(ValueError):
                selected_file({"files": [dict(self.file, url=url)]}, self.record)


if __name__ == "__main__":
    unittest.main()
