import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private final QuadTree finalTree = QuadTree.getFinalTree();
    List<QuadTree> listOfQuadtrees;
    private treeTraverser treeTraverser;
    private double[] toReturnLonsAndLats;
    private int depth;

    /**
     * imgRoot is the name of the directory containing the images.
     * You may not actually need this for your class.
     */
    public Rasterer(String imgRoot) { }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     * The grid of images must obey the following properties, where image in the
     * grid is referred to as a "tile".
     * <ul>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * (LonDPP) possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * </p>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image
     * string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     * forget to set this to true! <br>
     * //     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        listOfQuadtrees = new ArrayList<>();
        toReturnLonsAndLats = new double[4];
        this.treeTraverser = new treeTraverser(params);
        unorderedRastering(finalTree);

        String[][] toReturn = creatingArray();
        Map<String, Object> results = new HashMap<>();

        results.put("render_grid", toReturn);
        results.put("raster_ul_lat", toReturnLonsAndLats[0]);
        results.put("raster_ul_lon", toReturnLonsAndLats[1]);
        results.put("raster_lr_lon", toReturnLonsAndLats[2]);
        results.put("raster_lr_lat", toReturnLonsAndLats[3]);
        results.put("depth", depth);
        results.put("query_success", true);
        return results;
    }

    public void unorderedRastering(QuadTree tree) {
        boolean check1 = treeTraverser.intersectsTiles2
                (tree.getULLAT(), tree.getULLON(), tree.getLRLAT(), tree.getLRLON());

        boolean check2 = treeTraverser.lonDPPsmallerThanOrIsLeaf
                (tree.getLONDPP(), tree.getDepthOfTree());

        if (!check1) {
            return;
        } else {
            if (check1 && !check2) {
                unorderedRastering(tree.getChild1());
                unorderedRastering(tree.getChild2());
                unorderedRastering(tree.getChild3());
                unorderedRastering(tree.getChild4());
            }
        }
        if (check1 && check2) {
            listOfQuadtrees.add(tree);
        }
    }

    public String[][] creatingArray() {
        Collections.sort(listOfQuadtrees, new QuadtreeCompararator());
        toReturnLonsAndLats[0] = listOfQuadtrees.get(0).getULLAT();
        toReturnLonsAndLats[1] = listOfQuadtrees.get(0).getULLON();
        toReturnLonsAndLats[2] = listOfQuadtrees.get(listOfQuadtrees.size() - 1).getLRLON();
        toReturnLonsAndLats[3] = listOfQuadtrees.get(listOfQuadtrees.size() - 1).getLRLAT();
        depth = listOfQuadtrees.get(0).getDepthOfTree();

        double nodeLat = listOfQuadtrees.get(0).getULLAT() - listOfQuadtrees.get(0).getLRLAT();
        double nodeLon = listOfQuadtrees.get(0).getULLON() - listOfQuadtrees.get(0).getLRLON();

        int rows = (int) Math.round((toReturnLonsAndLats[0] - toReturnLonsAndLats[3]) / nodeLat);
        int columns = (int) Math.round((toReturnLonsAndLats[1] - toReturnLonsAndLats[2]) / nodeLon);

        String[][] toReturn = new String[rows][columns];
        int count = 0;
        for (int i = 0; i < toReturn.length; i++) {
            for (int j = 0; j < toReturn[0].length; j++) {
                toReturn[i][j] = "img/" + listOfQuadtrees.get(count).getFileName() + ".png";
                count++;
            }
        }
        return toReturn;
    }
}
