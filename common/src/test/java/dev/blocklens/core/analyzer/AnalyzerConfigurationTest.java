package dev.blocklens.core.analyzer;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ui.SettingsDraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AnalyzerConfigurationTest {
    @TempDir Path directory;

    @Test void everyNumericLimitRoundTripsWhileLegacyBitsAndComfortRemainIntact() {
        for (boolean maximum : new boolean[]{false,true}) {
            var options=AnalyzerOptions.defaults();
            for(int kind=0;kind<5;kind++) for(int field=0;field<4;field++) {
                if(kind==0 && field==1) continue;
                options=options.with(kind,field,maximum?AnalyzerOptions.maximum(kind,field):AnalyzerOptions.minimum(kind,field));
            }
            var config=BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL,true).withAnalyzerOptions(options);
            var encoded=BlockLensConfigCodec.encode(config);
            var decoded=BlockLensConfigCodec.decode(encoded);
            assertEquals(config.enabledMask(),decoded.enabledMask());
            assertEquals(config.comfortOptions(),decoded.comfortOptions());
            assertEquals(options,decoded.analyzerOptions());
            assertFalse(encoded.contains("analyzer.debris_analyzer.vertical_radius="));
        }
    }

    @Test void MalformedOrOutOfRangeValuesPreserveTheLastValidField() {
        StringBuilder encoded=new StringBuilder();
        for(int kind=0;kind<5;kind++) for(int field=0;field<4;field++) {
            if(kind==0 && field==1) continue;
            String key=AnalyzerCatalog.optionKey(kind,field);
            encoded.append(key).append('=').append(AnalyzerOptions.minimum(kind,field)).append('\n');
            for(String invalid:new String[]{"bad","2147483648",Integer.toString(AnalyzerOptions.minimum(kind,field)-1),
                    Integer.toString(AnalyzerOptions.maximum(kind,field)+1)}) {
                encoded.append(key).append('=').append(invalid).append('\n');
            }
        }
        var decoded=BlockLensConfigCodec.decode(encoded.toString());
        for(int kind=0;kind<5;kind++) for(int field=0;field<4;field++) {
            assertEquals(AnalyzerOptions.minimum(kind,field),decoded.analyzerOptions().value(kind,field));
        }
        assertEquals(BlockLensConfig.defaults().enabledMask(),decoded.enabledMask());
        assertEquals(0,AnalyzerCatalog.mask(decoded));
        assertEquals(AnalyzerOptions.defaults(),BlockLensConfigCodec.decode("deco.anvil=true\n").analyzerOptions());
    }

    @Test void NumericOnlyEditsDiscardAndPersistThroughTheExistingDraft() throws IOException {
        var draft=new SettingsDraft(BlockLensConfig.defaults());
        var options=draft.config().analyzerOptions().with(0,0,16).with(4,2,24);
        draft.setAnalyzerOptions(options);
        assertTrue(draft.changed());
        draft.discard();
        assertFalse(draft.changed());
        assertEquals(AnalyzerOptions.defaults(),draft.config().analyzerOptions());
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)));
        draft.setAnalyzerOptions(options);
        assertTrue(draft.save(directory));
        assertFalse(draft.changed());
        assertEquals(options,BlockLensConfigFiles.loadOrCreate(directory).analyzerOptions());
        assertEquals(BlockLensConfig.defaults().enabledMask(),draft.config().enabledMask());
    }

    @Test void FailedSavingKeepsTheWholeNumericDraftForRetry() throws IOException {
        var draft=new SettingsDraft(BlockLensConfig.defaults());
        var options=AnalyzerOptions.defaults().with(1,0,8).with(2,2,16);
        draft.setAnalyzerOptions(options);
        draft.toggle(CapabilityId.LAVA_ANALYZER);
        var expectedMask=draft.config().enabledMask();
        var regularFile=directory.resolve("regular-file");
        Files.writeString(regularFile,"fixture");
        assertThrows(IOException.class,()->draft.save(regularFile));
        assertTrue(draft.changed());
        assertEquals(options,draft.config().analyzerOptions());
        assertEquals(expectedMask,draft.config().enabledMask());
        assertTrue(draft.save(directory));
        var loaded=BlockLensConfigFiles.loadOrCreate(directory);
        assertEquals(options,loaded.analyzerOptions());
        assertEquals(expectedMask,loaded.enabledMask());
    }
}