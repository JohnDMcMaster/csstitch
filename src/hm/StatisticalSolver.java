/*
Copyright Christian Sattler <sattler.christian@gmail.com>
Modifications by John McMaster <JohnDMcMaster@gmail.com>
*/

package hm;

import general.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import map.properties.ImagePosition;
import map.properties.ImageSetProperties;
import map.properties.ImageSize;
import map.properties.OpticalProperties;
import map.properties.PerspectiveProperties;

import stitcher.Stitcher4;
import tools.LinearEquation;
import tools.SumOfSquares;
import hm.ImageCoordinateMap;

//Pair<Integer, Integer> = Pair<Integer, Integer>
public class StatisticalSolver {
    
    private int sx, sy;
    private int numCoefs;
    
    private ImageCoordinateMap[] names;
    private TreeMap<String, Pair<Integer, Integer>>[] nameLookup;
    
    private TreeMap<Pair<Integer, Integer>, Integer>[] indices;
    private TreeMap<Integer, Pair<Integer, Integer>>[] indexLookup;
    
    private String[] matchDir;
    
    private TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, ArrayList<double[]>>[] controlPoints;
    private int numControlPoints;
    private TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, TreeSet<Integer>>[] overlaps;
    
    private double[][] factors;
    private double[] errors;
    
    private double[] values;
    
    private void loadControlPoints(TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] neighbours, double[] probs, double minDistX,
            double minDistY) throws IOException {
        controlPoints = new TreeMap[names.length];
        overlaps = new TreeMap[names.length];
        numControlPoints = 0;
        
        for (int i = 0; i != names.length; ++i) {
            controlPoints[i] = new TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, ArrayList<double[]>>();
            
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> key : neighbours[i]) {
                LinkedList<double[]> list =
                        new LinkedList<double[]>(StitchTools.readControlPoints(new File(matchDir[i] + "/"
                                + names[i].m_map.get(key.getA()) + "-" + names[i].m_map.get(key.getB()) + ".pto")));
                
                for (Iterator<double[]> it = list.iterator(); it.hasNext();) {
                    double[] p = it.next();
                    if (!(Math.random() < probs[i]))
                        it.remove();
                    else if (minDistX != 0 && key.getA().getClass() == Pair.class) {
                        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> k =
                                (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>) (Object)key;

                        int dx = Integer.signum(k.getA().getA() - k.getB().getA());
                        int dy = Integer.signum(k.getA().getB() - k.getB().getB());
                        int dxx = (int) Math.signum(p[0] - p[2] + dy * minDistX);
                        int dyy = (int) Math.signum(p[1] - p[3] + dx * minDistY);
                        if ((dy != 0 && dy != -dxx) || (dx != 0 && dx != -dyy))
                            it.remove();
                    }
                }
                
                // use if there are too many control points
                int maxNum = 300;
                if (list.size() > maxNum) {
                    double quantil = (double) maxNum / list.size();
                    for (Iterator<double[]> it = list.iterator(); it.hasNext();) {
                        it.next();
                        if (!(Math.random() < quantil))
                            it.remove();
                    }
                }
                
                /* HACK
                for (Iterator<double[]> it = list.iterator(); it.hasNext();) {
                    double[] point = it.next();
                    Pair<Integer, Integer> a = (Pair<Integer, Integer>) key.getA();
                    Pair<Integer, Integer> b = (Pair<Integer, Integer>) key.getB();
                    if (a.getA() < b.getA()) {
                        if (point[1] < point[3])
                            it.remove();
                    } else {
                        if (point[0] < point[2])
                            it.remove();
                        else if (a.getB() != 0
                                && names.keySet().contains(new Pair<Integer, Integer>(b.getA(), b.getB() + 1))) {
                            if (point[0] - 0.6 * sx < point[2])
                                it.remove();
                        }
                    }
                }
                //*/

                controlPoints[i].put(key, new ArrayList<double[]>(list));
            }
            
            overlaps[i] = new TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, TreeSet<Integer>>();
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> key : neighbours[i]) {
                int size = controlPoints[i].get(key).size();
                
                TreeSet<Integer> set = new TreeSet<Integer>();
                for (int j = 0; j != size; ++j)
                    set.add(numControlPoints++);
                
                if (!set.isEmpty())
                    overlaps[i].put(key, set);
            }
        }
        
        int numNames = 0;
        for (int i = 0; i != names.length; ++i)
            numNames += names[i].size() + 2;
        
        errors = new double[numControlPoints];
        values = new double[2 * numNames + numCoefs];
        
        factors = new double[2 * errors.length][values.length];
        
        int k = 0;
        int nameAcc = 0;
        
        for (int i = 0; i != names.length; ++i) {
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> p : controlPoints[i].keySet()) {
                for (double[] point : controlPoints[i].get(p)) {
                    double x0 = point[0] - 0.5 * (sx - 1);
                    double y0 = point[1] - 0.5 * (sy - 1);
                    
                    double x1 = point[2] - 0.5 * (sx - 1);
                    double y1 = point[3] - 0.5 * (sy - 1);
                    
                    double r0sq = x0 * x0 + y0 * y0;
                    double r1sq = x1 * x1 + y1 * y1;
                    
                    double r0pow = 1;
                    double r1pow = 1;
                    
                    for (int j = 0; j != numCoefs; ++j) {
                        factors[2 * k + 0][2 * numNames + j] = x0 * r0pow - x1 * r1pow;
                        factors[2 * k + 1][2 * numNames + j] = y0 * r0pow - y1 * r1pow;
                        
                        r0pow *= r0sq;
                        r1pow *= r1sq;
                    }
                    
                    factors[2 * k + 0][2 * (nameAcc + indices[i].get(p.getA())) + 0] = 1;
                    factors[2 * k + 1][2 * (nameAcc + indices[i].get(p.getA())) + 1] = 1;
                    
                    factors[2 * k + 0][2 * (nameAcc + indices[i].get(p.getB())) + 0] = -1;
                    factors[2 * k + 1][2 * (nameAcc + indices[i].get(p.getB())) + 1] = -1;
                    
                    factors[2 * k + 0][2 * (nameAcc + names[i].size()) + 0] = x0 * x0 - x1 * x1;
                    factors[2 * k + 1][2 * (nameAcc + names[i].size()) + 0] = x0 * y0 - x1 * y1;
                    
                    factors[2 * k + 0][2 * (nameAcc + names[i].size()) + 1] = y0 * x0 - y1 * x1;
                    factors[2 * k + 1][2 * (nameAcc + names[i].size()) + 1] = y0 * y0 - y1 * y1;
                    
                    ++k;
                }
            }
            
            nameAcc += names[i].size() + 2;
        }
        
        values[2 * nameAcc] = 1;
        //values[2 * nameAcc + 1] = -5.35E9;
    }
    
    private void computeErrors(Set<Integer> selection) {
        for (int i = 0; i != errors.length; ++i)
            errors[i] = 0;
        
        for (int i : selection) {
            for (int j = 0; j != 2; ++j) {
                double a = 0;
                for (int k = 0; k != values.length; ++k)
                    a += factors[2 * i + j][k] * values[k];
                
                errors[i] += a * a;
            }
            errors[i] = Math.sqrt(errors[i]);
        }
    }
    
    private double[][] prepareSummandRoots(Set<Integer> scaledSelection, Set<Integer> variables) {
        double[][] summandRoots = new double[scaledSelection.size()][variables.size() + 1];
        int i = 0;
        for (int j : scaledSelection) {
            summandRoots[i][variables.size()] = 0;
            
            int k = 0;
            for (int l = 0; l != values.length; ++l)
                if (variables.contains(l))
                    summandRoots[i][k++] = factors[j][l];
                else
                    summandRoots[i][variables.size()] += factors[j][l] * values[l];
            
            ++i;
        }
        
        return summandRoots;
    }
    
    private void solve(Set<Integer> scaledSelection, Set<Integer> variables) {
        Integer[] varMap = variables.toArray(new Integer[] {});
        
        TreeSet<Integer> nonVars = new TreeSet<Integer>();
        for (int i = 0; i != values.length; ++i)
            nonVars.add(i);
        
        nonVars.removeAll(variables);
        Integer[] nonVarMap = nonVars.toArray(new Integer[] {});
        
        double[][] a = new double[varMap.length][varMap.length];
        double[] c = new double[varMap.length];
        
        for (int i : scaledSelection)
            for (int j = 0; j != varMap.length; ++j) {
                for (int k = 0; k != varMap.length; ++k)
                    a[j][k] += factors[i][varMap[j]] * factors[i][varMap[k]];
                
                double constant = 0;
                for (int k : nonVarMap)
                    constant -= factors[i][k] * values[k];
                
                c[j] += factors[i][varMap[j]] * constant;
            }
        
        double[] newValues = LinearEquation.solveLinearEquation(a, c, 0, false);
        for (int j = 0; j != varMap.length; ++j)
            values[varMap[j]] = newValues[j];
    }
    
    private void sieveOverlaps(Set<Integer> selection, double errorThreshold, boolean verbose) {
        int nameAcc = 0;
        
        for (int i = 0; i != names.length; ++i) {
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> p : overlaps[i].keySet()) {
                TreeSet<Integer> variables = new TreeSet<Integer>();
                variables.add(2 * (nameAcc + indices[i].get(p.getB())) + 0);
                variables.add(2 * (nameAcc + indices[i].get(p.getB())) + 1);
                
                TreeSet<Integer> sel = new TreeSet<Integer>(overlaps[i].get(p));
                sel.retainAll(selection);
                
                TreeSet<Integer> scaledSelection = StitchTools.scaleSelection(sel);
                
                double[][] summandRoots = prepareSummandRoots(scaledSelection, variables);
                TreeSet<Integer> result = new TreeSet<Integer>();
                if (summandRoots.length != 0)
                    result =
                            SumOfSquares
                                    .iterativelySolveSumOfSquares2D(summandRoots, errorThreshold, 0, 0, false);
                
                int a = sel.size();
                
                Integer[] selArray = sel.toArray(new Integer[] {});
                for (int j : result)
                    sel.remove(selArray[j]);
                
                selection.removeAll(sel);
                
                int b = sel.size();
                
                if (verbose)
                    System.err.printf("%s - %s: %05.2f filtered (%d / %d)\n", p.getA().toString(), p.getB()
                            .toString(), 100. * b / a, b, a);
            }
            
            nameAcc += names[i].size() + 2;
            
            if (verbose)
                System.err.println();
        }
    }
    
    private void solveForCoord(Set<Integer> selection, int j, int coord) {
        TreeSet<Integer> scaledSelection = new TreeSet<Integer>();
        for (int i : selection)
            scaledSelection.add(2 * i + coord);
        
        int nameAcc = 0;
        for (int i = 0; i != j; ++i)
            nameAcc += names[i].size() + 2;
        
        TreeSet<Integer> variables = new TreeSet<Integer>();
        values[coord] = 0;
        for (int i = 1; i != names[j].size(); ++i)
            variables.add(2 * (nameAcc + i) + coord);
        
        solve(scaledSelection, variables);
    }
    
    private void solveForPositions(Set<Integer> selection, int j) {
        for (int coord = 0; coord != 2; ++coord)
            solveForCoord(selection, j, coord);
    }
    
    private void solveForLenseParameters(Set<Integer> selection) {
        int numNames = 0;
        for (int i = 0; i != names.length; ++i)
            numNames += names[i].size() + 2;
        
        TreeSet<Integer> variables = new TreeSet<Integer>();
        for (int i = 1; i != numCoefs; ++i)
            variables.add(2 * numNames + i);
        
        solve(StitchTools.scaleSelection(selection), variables);
    }
    
    private void solvePositionPerspective(Set<Integer> selection, int j) {
        TreeSet<Integer> scaledSelection = StitchTools.scaleSelection(selection);
        
        TreeSet<Integer> variables = new TreeSet<Integer>();
        
        int nameAcc = 0;
        for (int i = 0; i != j; ++i)
            nameAcc += names[i].size() + 2;
        
        variables.add(nameAcc + 2 * names[j].size() + 0);
        variables.add(nameAcc + 2 * names[j].size() + 1);
        
        for (int i = 1; i != names[j].size(); ++i) {
            variables.add(2 * (nameAcc + i) + 0);
            variables.add(2 * (nameAcc + i) + 1);
        }
        
        solve(scaledSelection, variables);
    }
    
    private void solveEverything(Set<Integer> selection) {
        int numNames = 0;
        for (int i = 0; i != names.length; ++i)
            numNames += names[i].size() + 2;
        
        TreeSet<Integer> variables = new TreeSet<Integer>();
        int nameAcc = 0;
        for (int i = 0; i != names.length; ++i) {
            values[2 * nameAcc + 0] = 0;
            values[2 * nameAcc + 1] = 0;
            
            for (int j = 2; j != 2 * names[i].size(); ++j)
                variables.add(2 * nameAcc + j);
            
            nameAcc += names[i].size() + 2;
        }
        
        for (int i = 1; i != numCoefs; ++i)
            variables.add(2 * numNames + i);
        
        solve(StitchTools.scaleSelection(selection), variables);
    }
    
    private void doOverlapSievingStep(Set<Integer> selection, double threshold) {
        sieveOverlaps(selection, threshold, false);
        
        System.err.println("filtering with threshold " + threshold + " leaves " + selection.size()
                + " control points");
        System.err.println();
    }
    
    private void filterPoints(Set<Integer> selection, double threshold) {
        for (int i : new TreeSet<Integer>(selection))
            if (errors[i] > threshold)
                selection.remove(i);
        
        System.err.println(selection.size() + " points remaining");
        System.err.println();
    }
    
    private void doModernFiltering(Set<Integer> selection, double threshold) {
        int numNames = 0;
        for (int i = 0; i != names.length; ++i)
            numNames += names[i].size() + 2;
        
        TreeSet<Integer> variables = new TreeSet<Integer>();
        int nameAcc = 0;
        for (int i = 0; i != names.length; ++i) {
            values[2 * nameAcc + 0] = 0;
            values[2 * nameAcc + 1] = 0;
            
            for (int j = 2; j != 2 * names[i].size(); ++j)
                variables.add(2 * nameAcc + j);
            
            nameAcc += names[i].size() + 2;
        }
        
        double[][] summandRoots = prepareSummandRoots(StitchTools.scaleSelection(selection), variables);
        System.err.println("summands prepared");
        
        TreeSet<Integer> result =
                SumOfSquares.iterativelySolveSumOfSquares2D(summandRoots, threshold, 0, 0,
                        0.01 /* USED TO BE: 0.01, use -1 for single-stepping */, true);
        
        Integer[] selArray = selection.toArray(new Integer[] {});
        selection.clear();
        for (int i : result)
            selection.add(selArray[i]);
        
        System.err.println(selection.size() + " points remaining");
        System.err.println();
    }
    
    private void outputPositions() {
if( true )
return;
        int nameAcc = 0;
        for (int i = 0; i != names.length; ++i) {
        	System.out.printf("names[i].size(): %d\n", names[i].size());
            for (int image = names[i].size() - 5; image != names[i].size(); ++image)
                System.err.printf("(%06.2f, %06.2f)\n", values[2 * (nameAcc + image) + 0],
                        values[2 * (nameAcc + image) + 1]);
            System.err.println();
            
            nameAcc += names[i].size() + 2;
        }
        System.err.println();
    }
    
    private void outputLenseParameters() {
        int numNames = 0;
        for (int i = 0; i != names.length; ++i)
            numNames += names[i].size() + 2;
        
        for (int i = 0; i != numCoefs; ++i)
            System.err.printf("%010.5f, ",
                    values[2 * numNames + i] * (Math.pow((sx * sx + sy * sy) / 4, i)));
        System.err.println();
        System.err.println();
    }
    
    private void checkOverlaps(Set<Integer> selection) {
        for (int i = 0; i != names.length; ++i) {
            TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, double[]> shifts = new TreeMap<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, double[]>();
            
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> p : overlaps[i].keySet()) {
                TreeSet<Integer> set = new TreeSet<Integer>(overlaps[i].get(p));
                set.retainAll(selection);
                if (set.isEmpty())
                    continue;
                
                int numPoints = set.size();
                double[] shift = new double[2];
                
                for (int r : set) {
                    for (int j = 0; j != 2; ++j) {
                        double z = 0;
                        for (int k = 0; k != values.length; ++k)
                            z -= factors[2 * r + j][k] * values[k];
                        shift[j] += z;
                    }
                }
                
                for (int r = 0; r != 2; ++r)
                    shift[r] /= numPoints;
                
                shifts.put(p, new double[] {shift[0], shift[1], numPoints});
            }
            
            int minNumPoints = Integer.MAX_VALUE, maxNumPoints = 0;
            double mean = 0, deviation = 0, max = 0;
            Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> maxIndex = null, minNumPointsIndex = null;
            for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> p : shifts.keySet()) {
                double[] s = shifts.get(p);
                double e = Math.sqrt(s[0] * s[0] + s[1] * s[1]);
                mean += e;
                deviation += e * e;
                
                if (e > max) {
                    max = e;
                    maxIndex = p;
                    maxNumPoints = (int) s[2];
                }
                
                if (s[2] < minNumPoints) {
                    minNumPoints = (int) s[2];
                    minNumPointsIndex = p;
                }
            }
            
            mean /= shifts.size();
            deviation = Math.sqrt(deviation / shifts.size());
            
            System.err.println("mean " + mean + ", deviation " + deviation + ", max " + max + " ("
                    + maxIndex + ", " + maxNumPoints + "points)");
            System.err.println("min #points " + minNumPoints + " (" + minNumPointsIndex + ")");
            System.err.println();
        }
        
        System.err.println();
    }
    
    /*
    private void doTraditionalAnalysis(Set<Integer> selection) {
        solveForPositions(selection);
        solveForLenseParameters(selection);
        
        computeErrors(selection);
        SumOfSquares.outputErrorInfo(selection, errors);
        
        checkOverlaps(selection);
    }*/

    private void doModernAnalysis(Set<Integer> selection) {
        solveEverything(selection);
        
        outputPositions();
        outputLenseParameters();
        
        computeErrors(selection);
        SumOfSquares.outputErrorInfo(selection, errors);
        
        checkOverlaps(selection);
    }
    
    private ImageSetProperties outputParameters(int i) throws IOException {
        ImageSize size = new ImageSize(sx, sy);
        
        int nameAcc = 0;
        for (int j = 0; j != i; ++j)
            nameAcc += names[j].size() + 2;
        
        ImagePosition[] positions = new ImagePosition[names[i].size()];
        for (int j = 0; j != positions.length; ++j)
            positions[j] =
                    new ImagePosition(values[2 * (nameAcc + j) + 0], values[2 * (nameAcc + j) + 1]);
        
        int numNames = 0;
        for (int j = 0; j != names.length; ++j)
            numNames += names[j].size() + 2;
        
        OpticalProperties[] opticalProperties = new OpticalProperties[1];
        opticalProperties[0] =
                new OpticalProperties(Arrays.copyOfRange(values, 2 * numNames, 2 * numNames + numCoefs));
        
        PerspectiveProperties perspectiveProperties =
                new PerspectiveProperties(values[2 * (nameAcc + names[i].size()) + 0],
                        values[2 * (nameAcc + names[i].size()) + 1]);
        
        return new ImageSetProperties(size, positions, opticalProperties, perspectiveProperties);
    }
    
    public ImageSetProperties analyze() throws IOException {
        return analyze(false)[0];
    }
    
    public ImageSetProperties[] analyze(boolean dummy) throws IOException {
        System.err.println("loaded " + factors.length / 2 + " control points");
        System.err.println();
        
        TreeSet<Integer> selection = new TreeSet<Integer>();
        for (int i = 0; i != factors.length / 2; ++i)
            //if (Math.random() < 0.25)
            selection.add(i);
        
        //doOverlapSievingStep(selection, 20);
        //doModernFiltering(selection, 2000);
        //doModernFiltering(selection, 1000);
        //doModernFiltering(selection, 500);
        //doModernFiltering(selection, 200);
        //doModernFiltering(selection, 100);
        //doModernFiltering(selection, 50);
        //doModernFiltering(selection, 20);
        //doModernAnalysis(selection);
        
        int nameAcc = 0;
        for (int i = 0; i != names.length; ++i)
            nameAcc += 2 * names[i].size() + 2;
        values[nameAcc + 1] = -1.5831948709060398E-8;
        
        doModernFiltering(selection, 10);
        doModernAnalysis(selection);
        solvePositionPerspective(selection, 0);
        
        doModernFiltering(selection, 5);
        doModernAnalysis(selection);
        solvePositionPerspective(selection, 0);
        
        doModernFiltering(selection, 3);
        doModernAnalysis(selection);
        solvePositionPerspective(selection, 0);
        
        //doModernFiltering(selection, 2);
        //doModernAnalysis(selection);
        
        ImageSetProperties[] sets = new ImageSetProperties[names.length];
        for (int i = 0; i != sets.length; ++i)
            sets[i] = outputParameters(i);
        
        return sets;
    }
    
    /*
	-    public StatisticalSolver(int sx, int sy, int numCoefs, TreeMap<A, String> names,
	-            TreeSet<Pair<A, A>> neighbours, String matchDir) throws IOException {
	*/
    public StatisticalSolver(int sx, int sy, int numCoefs, ImageCoordinateMap names,
            TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> neighbours, String matchDir) throws IOException {
        this(sx, sy, numCoefs, new ImageCoordinateMap[] {names}, new TreeSet[] {neighbours},
                new String[] {matchDir}, new double[] {1}, 0, 0);
    }
    
	/*     
	-    public StatisticalSolver(int sx, int sy, int numCoefs, TreeMap<A, String>[] names,
	-            TreeSet<Pair<A, A>>[] neighbours, String[] matchDir, double[] probs, double minDistX,
    */
    public StatisticalSolver(int sx, int sy, int numCoefs, ImageCoordinateMap[] namesIn,
            TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] neighbours, String[] matchDir, double[] probs, double minDistX,
            double minDistY) throws IOException {
        this.sx = sx;
        this.sy = sy;
        this.numCoefs = numCoefs;
        
        names = namesIn.clone();
        nameLookup = new TreeMap[names.length];
        
        indices = new TreeMap[names.length];
        indexLookup = new TreeMap[names.length];
        
        this.matchDir = matchDir.clone();
        
        for (int i = 0; i != names.length; ++i) {
        	//??? This line causes a crash and seems odd in combination with the clone
        	//Clone maybe doesn't do deep copy on array elements?
        	System.out.println("array[i] type = " + names[i].getClass() + ": " + names[i].toString());
        	names[i] = new ImageCoordinateMap(names[i]);
            nameLookup[i] = StitchTools.reverse(names[i].m_map);
            
            indices[i] = StitchTools.getIndices(names[i].m_map.keySet());
            indexLookup[i] = StitchTools.getIndexLookup(names[i].m_map.keySet());
        }
        
        loadControlPoints(neighbours, probs, minDistX, minDistY);
    }    
}

