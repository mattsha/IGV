/**
 * Copyright (c) 2010-2011 by Fred Hutchinson Cancer Research Center.  All Rights Reserved.

 * This software is licensed under the terms of the GNU Lesser General
 * Public License (LGPL), Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.

 * THE SOFTWARE IS PROVIDED "AS IS." FRED HUTCHINSON CANCER RESEARCH CENTER MAKES NO
 * REPRESENTATIONS OR WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED,
 * INCLUDING, WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS,
 * WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL FRED HUTCHINSON CANCER RESEARCH
 * CENTER OR ITS TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR
 * ANY DAMAGES OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES, ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS,
 * REGARDLESS OF  WHETHER FRED HUTCHINSON CANCER RESEARCH CENTER SHALL BE ADVISED,
 * SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

/*
 *
 */
package org.broad.igv.sam;

import com.jidesoft.swing.JidePopupMenu;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.*;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.renderer.SpliceJunctionRenderer;
import org.broad.igv.track.*;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.renderer.DataRange;
import org.broad.tribble.Feature;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author dhmay
 *         Finds splice junctions in real time and renders them as Features
 */
public class SpliceJunctionFinderTrack extends FeatureTrack {

    private static Logger log = Logger.getLogger(SpliceJunctionFinderTrack.class);

    AlignmentDataManager dataManager;
    PreferenceManager prefs;
    RenderContext context;
    Genome genome;

    protected int minReadFlankingWidth = 0;
    protected int minJunctionCoverage = 1;


    public SpliceJunctionFinderTrack(String id, String name, AlignmentDataManager dataManager, Genome genome) {
        super(id, name);
        super.setDataRange(new DataRange(0, 0, 60));
        this.genome = genome;
        this.source = new SpliceJunctionFinderFeatureSource();
        setRendererClass(SpliceJunctionRenderer.class);
        this.dataManager = dataManager;
        prefs = PreferenceManager.getInstance();
    }


   
      @Override
      protected boolean isShowFeatures(RenderContext context)  {
          float maxRange = PreferenceManager.getInstance().getAsFloat(PreferenceManager.SAM_MAX_VISIBLE_RANGE);
          float minVisibleScale = (maxRange * 1000) / 700;
          return context.getScale() < minVisibleScale;

      }


    /**
     * Add a MenuItem to control the minimum flanking width for reads used in finding junctions.
     * If either the start OR the end flanking region is less than this, the read is not used
     *
     * @param menu
     * @return
     */
    public JMenuItem addFlankingWidthTresholdItem(JPopupMenu menu) {
        JMenuItem flankingWidthItem = new JMenuItem("Set minimum read flanking width...");

        flankingWidthItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                String value = JOptionPane.showInputDialog("Minimum start and end flanking region width: ",
                        Float.valueOf(minReadFlankingWidth));
                if (value == null) {
                    return;
                }
                try {
                    int tmp = Integer.parseInt(value);
                    minReadFlankingWidth = tmp;
                    IGV.getInstance().repaintDataPanels();
                }
                catch (Exception exc) {
                    //log
                }

            }
        });
        menu.add(flankingWidthItem);

        return flankingWidthItem;
    }


    /**
     * Add a MenuItem to control the minimum flanking width for reads used in finding junctions.
     * If either the start OR the end flanking region is less than this, the read is not used
     *
     * @param menu
     * @return
     */
    public JMenuItem addJunctionCoverageTresholdItem(JPopupMenu menu) {
        JMenuItem junctionDepthItem = new JMenuItem("Set minimum junction coverage...");

        junctionDepthItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                String value = JOptionPane.showInputDialog("Minimum coverage depth for displayed junctions: ",
                        Float.valueOf(minJunctionCoverage));
                if (value == null) {
                    return;
                }
                try {
                    int tmp = Integer.parseInt(value);
                    minJunctionCoverage = tmp;
                    IGV.getInstance().repaintDataPanels();
                }
                catch (Exception exc) {
                    //log
                }

            }
        });
        menu.add(junctionDepthItem);

        return junctionDepthItem;
    }

    /**
     * Override to return a specialized popup menu
     *
     * @return
     */
    @Override
    public JPopupMenu getPopupMenu(TrackClickEvent te) {

        JPopupMenu popupMenu = new JidePopupMenu();

        JLabel popupTitle = new JLabel("  " + getName(), JLabel.CENTER);

        Font newFont = popupMenu.getFont().deriveFont(Font.BOLD, 12);
        popupTitle.setFont(newFont);
        if (popupTitle != null) {
            popupMenu.add(popupTitle);
        }

        popupMenu.addSeparator();

        ArrayList<Track> tmp = new ArrayList();
        tmp.add(this);
        TrackMenuUtils.addStandardItems(popupMenu, tmp, te);
        popupMenu.addSeparator();
        addFlankingWidthTresholdItem(popupMenu);
        addJunctionCoverageTresholdItem(popupMenu);
        return popupMenu;
    }


    @Override
    public void setDataRange(DataRange axisDefinition) {
        // Explicitly setting a data range turns off auto-scale
        super.setDataRange(axisDefinition);
    }


    public boolean isLogNormalized() {
        return false;
    }

    public float getRegionScore(String chr, int start, int end, int zoom, RegionScoreType type, ReferenceFrame frame) {
        return 0;
    }



    class SpliceJunctionFinderFeatureSource implements FeatureSource {

        List<Feature> features = null;

        public Iterator getFeatures(String chr, int start, int end) throws IOException {
            final List<SpliceJunctionFeature> junctions = dataManager.getSpliceJunctions(genome.getId(), chr, start, end);
            return junctions == null ? null : junctions.iterator();
        }


        public List<LocusScore> getCoverageScores(String chr, int start, int end, int zoom) {
            return null;
        }

        public int getFeatureWindowSize() {
            return 0;
        }

        public void setFeatureWindowSize(int size) {

        }

        public Class getFeatureClass() {
            return null;
        }
    }


}