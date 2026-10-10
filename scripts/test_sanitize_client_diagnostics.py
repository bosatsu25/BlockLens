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
