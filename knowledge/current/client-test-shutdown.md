# Client GameTest shutdown

Status: authoritative test-harness contract (Issue #82).

The 26.3 Vulkan main CI at f443a2b repeatedly stopped at world_close. Bounded diagnostics show the render thread waiting for IntegratedServer.halt's server task while the server and test threads wait for Fabric's tick Phaser. This matches the mechanism reported in [Fabric API issue #5623](https://github.com/FabricMC/fabric-api/issues/5623); it is a test-harness failure, not evidence of a settings-button failure.

The repair is test-only and shared across the three targets. Immediately before the official world.close(), enqueue one bounded server task that continues managedBlock task processing until halt sets isRunning false. Do not synchronously wait for that task to start or insert a test tick between enqueue and close. Preserve the ordinary Fabric close, persistence, disconnected-state checks, and normal process exit.

A pure task guard records start, completion, cancellation, failure and expiry. A 30-second bound releases the pump but expiry is always failure. Close exceptions cancel the pump; no added thread, executor, runtime mixin, or product initialization is permitted. No production JAR or 200 KiB budget changes are intended.

Verification must cover the queued-halt race, expiry, cancellation, unstarted/incomplete tasks and exceptions; compile/build all three targets; exercise controlled delayed close followed by reopening the same saved world and state retention; require world_closed/passed evidence and process exit in the external Vulkan profile. Existing CI timeouts and acceptance gates remain unchanged.

Implementation uses a shared AutoCloseable holder around the official, non-extendable Fabric context. Its delegate remains unchanged, avoiding the getClientLevel/getConnection API difference between 26.1.2 and newer Fabric targets. Normal, focused, external and BBE test worlds share this close path.

The real-client regression creates a fixed diamond block, delays exactly one client tick by 250 ms at each close, and reopens the same save twice to assert the block persists. Callback counts must increase by exactly one per close before the delayed_close marker may be reported. The task guard must finish without expiry and the server must be shut down with the client level unloaded. The callback exists only in the GameTest source set and is inactive outside the three controlled closes.

Failure cleanup cancels the queued pump without replacing the original exception. This is a bounded release condition, not a claim that every exceptional path completes immediately. Normal success requires observed task completion; an unfinished or expired task cannot produce successful evidence. No new background thread or executor is created.
