package org.lable.dynamicconfig.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TextFileGathererIT {
    @SuppressWarnings("unchecked")
    @Test
    public void testGatherConfigurationFiles() throws IOException {

        InputStream is = TextFileGatherer.gatherConfigurationFiles("configurationloader", true);

        Yaml yaml = new Yaml();
        Map<String, String> object = (Map<String, String>) yaml.load(is);

        assertThat(object.get("one"), is("XXX"));
        assertThat(object.get("two"), is("XXX"));
        assertThat(object.get("three"), is("XXX"));
    }

    @Test
    public void testGatherConfigurationFilesNoResources() throws IOException {
        InputStream is = TextFileGatherer.gatherConfigurationFiles("nothingherethisisafakepath", true);

        assertThat(IOUtils.toString(is).trim(), is(""));
    }

    @Test
    public void testLs() {
        Collection<String> files = TextFileGatherer.ls("configurationloader", ".*\\.yml");

        // Assert that the files provided in the testing resources are found:
        assertThat(files, hasItem("configurationloader/1-one.yml"));
        assertThat(files, hasItem("configurationloader/2-two.yml"));
        assertThat(files, hasItem("configurationloader/3-three.yml"));
        assertThat(files.size(), is(3));
    }
}