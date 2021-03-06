package org.broad.igv.hic;

import org.broad.igv.feature.Chromosome;
import org.broad.igv.hic.data.*;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * This is the "model" class for the HiC viewer.
 *
 * @author Jim Robinson
 * @date 4/8/12
 */
public class HiC {

    MainWindow mainWindow;
    MainWindow.DisplayOption displayOption;
    Dataset dataset;
    private Chromosome[] chromosomes;
    Map<Integer, DensityFunction> zoomToDensityMap = null;

    public Context xContext;
    Context yContext;

    Matrix matrix;
    public MatrixZoomData zd;


    public HiC(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public MainWindow.DisplayOption getDisplayOption() {
        return displayOption;
    }


    public boolean isWholeGenome() {
        return xContext != null && xContext.getChromosome().getName().equals("All");
    }


    public void reset() {
        matrix = null;
        zd = null;
    }


    public void setChromosomes(Chromosome[] chromosomes) {
        this.chromosomes = chromosomes;
    }

    public Chromosome[] getChromosomes() {
        return chromosomes;
    }


    public void setZoomToDensityMap(Map<Integer, DensityFunction> zoomToDensityMap) {
        this.zoomToDensityMap = zoomToDensityMap;
    }


    public Map<Integer, DensityFunction> getZoomToDensityMap() {
        return zoomToDensityMap;
    }

    /**
     * Return the expected density function for the current zoom level
     *
     * @return
     */
    public DensityFunction getDensityFunction() {
        return zd == null || zoomToDensityMap == null ? null : zoomToDensityMap.get(zd.getZoom());
    }

    /**
     * Return the expected density function for a specified zoom level.
     *
     * @param zoom
     * @return density function, or null if expected densities are not available
     */
    public DensityFunction getDensityFunction(int zoom) {
        return zoomToDensityMap == null ? null : zoomToDensityMap.get(zoom);
    }


    /**
     * Change zoom level and recenter.  Triggered by the resolutionSlider, or by a double-click in the
     * heatmap panel.
     *
     * @param newZoom
     * @param genomePositionX center X in base pairs
     * @param genomePositionY center Y in base pairs
     * @param updateSlider
     */
    public void setZoom(int newZoom, final double genomePositionX, final double genomePositionY, boolean updateSlider) {

        if (newZoom < 0 || newZoom > dataset.getNumberZooms()) return;

        final Chromosome chr1 = xContext.getChromosome();
        final Chromosome chr2 = yContext.getChromosome();
        final MatrixZoomData newZD = dataset.getMatrix(chr1, chr2).getObservedMatrix(newZoom);
        if (newZD == null) {
            JOptionPane.showMessageDialog(mainWindow, "Sorry, this zoom is not available", "Zoom unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (displayOption == MainWindow.DisplayOption.PEARSON && newZD.getPearsons() == null) {

            if (newZoom > 3) {
                int ans = JOptionPane.showConfirmDialog(mainWindow, "Pearson's calculation at " +
                        "this resolution will take a while.\nAre you sure you want to proceed?",
                        "Confirm calculation", JOptionPane.YES_NO_OPTION);
                if (ans == JOptionPane.NO_OPTION) {
                    mainWindow.updateZoom(zd.getZoom());
                    return;
                }
            }

            final DensityFunction df = getDensityFunction(newZoom);
            Runnable callable = new Runnable() {
                public void run() {
                    newZD.computePearsons(df);
                    //mainWindow.updateEigenvectorTrack();
                    mainWindow.refresh();
                    updateState2(newZD, genomePositionX, genomePositionY);
                }
            };
            mainWindow.executeLongRunningTask(callable);
        } else {
            //newZD.printDescription();
            updateState2(newZD, genomePositionX, genomePositionY);
        }

    }


    private void updateState2(MatrixZoomData newZD,  double genomePositionX, final double genomePositionY) {

        zd = newZD;
        int newZoom = zd.getZoom();
        //showEigenvector();

        //int newBinSize = zd.getBinSize();

       // double xScaleMax = (double) xContext.getChrLength() / mainWindow.getHeatmapPanel().getWidth();
       // double yScaleMax = (double) yContext.getChrLength() / mainWindow.getWidth();
       // double scaleMax = Math.max(xScaleMax, yScaleMax);

      //  double scale = Math.min(newBinSize, scaleMax);

        xContext.setZoom(newZoom);
        yContext.setZoom(newZoom);

        Point binPosition = zd.getBinPosition(genomePositionX, genomePositionY);

        center(binPosition.x, binPosition.y);
        mainWindow.updateZoom(newZoom);

        mainWindow.refresh();
    }


    /**
     * Zoom to a specific rectangle.  Triggered by the alt-drag action in the heatmap panel.
     *
     * @param xBP
     * @param yBP
     * @param scale
     */
    public void zoomTo(final double xBP, final double yBP, final double scale) {

        // Find zoom level closest to prescribed scale
        int newZoom = dataset.getNumberZooms() - 1;
        for (int z = 1; z < dataset.getNumberZooms(); z++) {
            if (dataset.getZoom(z) < scale) {
                newZoom = z - 1;
                break;
            }
        }

        final Chromosome chr1 = xContext.getChromosome();
        final Chromosome chr2 = yContext.getChromosome();
        final MatrixZoomData newZD = dataset.getMatrix(chr1, chr2).getObservedMatrix(newZoom);

        if (displayOption == MainWindow.DisplayOption.PEARSON && newZD.getPearsons() == null) {
            if (newZoom > 3) {
                int ans = JOptionPane.showConfirmDialog(mainWindow, "Pearson's calculation at " +
                        "this zoom will take a while.\nAre you sure you want to proceed?",
                        "Confirm calculation", JOptionPane.YES_NO_OPTION);
                if (ans == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            final DensityFunction df = getDensityFunction(newZoom);
            Runnable callable = new Runnable() {
                public void run() {
                    newZD.computePearsons(df);
                    mainWindow.refresh();
                    updateState(newZD, scale, xBP, yBP);
                }
            };
            mainWindow.executeLongRunningTask(callable);
        } else {
            updateState(newZD, scale, xBP, yBP);

        }
    }


    private void updateState(MatrixZoomData newZD, double scale, double xBP, double yBP) {
        zd = newZD;
        xContext.setZoom(zd.getZoom(), (int) scale);
        yContext.setZoom(zd.getZoom(), (int) scale);
        xContext.setGenomicOrigin((int) xBP);
        yContext.setGenomicOrigin((int) yBP);
        mainWindow.updateZoom(zd.getZoom());
        mainWindow.refresh();
    }

    public void center(int binX, int binY) {
        double w = mainWindow.getHeatmapPanel().getWidth();
        int newX = (int) (binX - w / 2);
        double h = mainWindow.getHeatmapPanel().getHeight();
        int newY = (int) (binY - h / 2);
        moveTo(newX, newY);
    }


    public void moveBy(int dx, int dy) {
        final int newX = xContext.getBinOrigin() + dx;
        final int newY = yContext.getBinOrigin() + dy;
        moveTo(newX, newY);
    }

    private void moveTo(int newBinX, int newBinY) {

        final int w = mainWindow.getHeatmapPanel().getWidth();
        int maxX = zd.getMaxBinX() - w;

        final int h = mainWindow.getHeatmapPanel().getHeight();
        int maxY = zd.getMaxBinY() - h;

        int x = Math.max(0, Math.min(maxX, newBinX));
        int y = Math.max(0, Math.min(maxY, newBinY));

        xContext.setBinOrigin(x);
        yContext.setBinOrigin(y);

//        String locus1 = "chr" + (xContext.getChromosome().getName()) + ":" + x + "-" + (int) (x + bpWidthX);
//        String locus2 = "chr" + (yContext.getChromosome().getName()) + ":" + x + "-" + (int) (y + bpWidthY);
//        IGVUtils.sendToIGV(locus1, locus2);

        mainWindow.repaint();
    }


    public void setDisplayOption(MainWindow.DisplayOption newDisplay) {

        if (this.displayOption != newDisplay) {

            if (newDisplay == MainWindow.DisplayOption.PEARSON && zd.getPearsons() == null) {
                if (zd.getZoom() > 3) {
                    int ans = JOptionPane.showConfirmDialog(mainWindow, "Pearson's calculation at " +
                            "this zoom will take a while.\nAre you sure you want to proceed?",
                            "Confirm calculation", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        return;
                    }
                }

                this.displayOption = newDisplay;
                final DensityFunction df = getDensityFunction(zd.getZoom());
                Runnable callable = new Runnable() {
                    public void run() {
                        zd.computePearsons(df);
                        mainWindow.refresh();
                    }
                };
                mainWindow.executeLongRunningTask(callable);
                return;


            }
            this.displayOption = newDisplay;
            mainWindow.refresh();

        }
    }

    public double[] getEigenvector(final int n) {
        if (zd == null) return null;
        double[] eigenvector = zd.getEigenvector();
        if (eigenvector == null) {
            final DensityFunction df = getDensityFunction();
            if (df != null) {
                if (zd.getZoom() > 2) {
                    String str = "Eigenvector calculation requires Pearson's correlation matrix.\n";
                    str += "At this zoom, calculation might take a while.\n";
                    str += "Are you sure you want to proceed?";
                    int ans = JOptionPane.showConfirmDialog(mainWindow, str, "Confirm calculation", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        // mainWindow.setViewEigenvector(false);
                        return null;
                    }
                }
                Runnable runnable = new Runnable() {
                    public void run() {
                        zd.computeEigenvector(df, n);
                        mainWindow.updateEigenvectorTrack();
                    }
                };
                mainWindow.executeLongRunningTask(runnable);

            }
        }

        return eigenvector;
    }


}
