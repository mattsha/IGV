/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.broad.igv.feature;

import org.broad.igv.AbstractHeadlessTest;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jrobinso
 */
public class GenomeManagerTest extends AbstractHeadlessTest {

    static GenomeManager genomeManager;

    public GenomeManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractHeadlessTest.setUpClass();
        genomeManager = GenomeManager.getInstance();
    }

    @Test
    public void testGenerateGenomeList() throws Exception {
        File inDir = new File(TestUtils.DATA_DIR, "genomes");
        String outPath = TestUtils.DATA_DIR + "out/genomelist.txt";

        String rootPath = "http://igvdata.broadinstitute.org/genomes";

        genomeManager.generateGenomeList(inDir, rootPath, outPath);

        BufferedReader reader = new BufferedReader(new FileReader(outPath));
        int count = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            assertTrue(line.contains(rootPath + "/"));
            count++;
        }
        assertEquals(4, count);
    }

}