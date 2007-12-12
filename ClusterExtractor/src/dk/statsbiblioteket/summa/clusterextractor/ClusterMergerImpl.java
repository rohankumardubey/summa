/* $Id: ClusterMergerImpl.java,v 1.3 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 10:26:43 $
 * $Author: bam $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.clusterextractor;

import dk.statsbiblioteket.summa.clusterextractor.data.*;
import dk.statsbiblioteket.summa.clusterextractor.math.CoordinateComparator;
import dk.statsbiblioteket.summa.clusterextractor.math.IncrementalCentroid;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * ClusterMergerImpl merges centroid sets into a dendrogram.
 * The local builders push newly build centroid sets to the
 * merger. The merger merges the local centroid sets into one centroid set
 * and then into a dendrogram. The merged dendrogram is then
 * pushed to the known providers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterMergerImpl extends UnicastRemoteObject implements ClusterMerger {
    protected static final Log log = LogFactory.getLog(ClusterMergerImpl.class);
    /** Configurations. */
    protected Configuration conf;
    /** Merged Dendrogram. */
    private Dendrogram dendrogram;

    /**
     * Construct cluster merger using given configurations.
     * @param conf configurations
     * @throws java.rmi.RemoteException if failed to export object
     */
    public ClusterMergerImpl(Configuration conf) throws RemoteException {
        super();
        this.conf = conf;
        exportRemoteInterfaces();
    }

    public void uploadCentroidSet(String machineId, long handle, ClusterSet clusterSet) {
        String directoryPath = conf.getString(CLUSTER_SETS_PATH_KEY);
        File localCentroidSetDirectory = new File(directoryPath);
        if (!localCentroidSetDirectory.exists()) {
            localCentroidSetDirectory.mkdir();
        }
        long timeStamp = System.currentTimeMillis();
        File file = new File(directoryPath+timeStamp+"centroids.set");
        clusterSet.save(file);
    }

    public void mergeCentroidSets() {
        ClusterSet centroids = loadCentroids();
        centroids = resolveNameConflicts(centroids);
        this.dendrogram = buildDendrogram(centroids);
        saveDendrogram(dendrogram);
    }

    public Dendrogram getNewDendrogram() {
        return this.dendrogram;
    }

    /**
     * Save given dendrogram in directory specified in properties.
     * @param dendrogram dendrogram to save
     */
    private void saveDendrogram(Dendrogram dendrogram) {
        String localDendrogramPath = conf.getString(DENDROGRAM_PATH_KEY);
        File localCentroidSetDirectory = new File(localDendrogramPath);
        if (!localCentroidSetDirectory.exists()) {
            localCentroidSetDirectory.mkdir();
        }
        long timeStamp = System.currentTimeMillis();
        File file = new File(localDendrogramPath+timeStamp+"dendrogram.obj");
        dendrogram.save(file);
    }

    /**
     * Build {@link Dendrogram} from given centroid set.
     * @param centroids centroid set
     * @return dendrogram
     */
    private Dendrogram buildDendrogram(ClusterSet centroids) {
        if (centroids==null || centroids.isEmpty()) {
            log.warn("ClusterMergerImpl buildDendrogram centroid set empty.");
            return null;
        }

        //get join similarity threshold
        double value;
        try {
            value = Double.parseDouble(conf.getString(JOIN_SIMILARITY_THRESHOLD_KEY));
        } catch (NumberFormatException e) {
            log.warn("ClusterMergerImpl.buildDendrogram: The " +
                    "double property with key " + JOIN_SIMILARITY_THRESHOLD_KEY +
                    "could not be parsed.", e);
            value = .9;
        }
        double joinSimThreshold =
                value;

        //initialise work array; note that as we are building a binary tree
        //bottom up, the total number of nodes is twice the starting number
        DendrogramNode[] workArray = new DendrogramNode[centroids.size()*2+1];
        int size = centroids.size();
        int index = 0;
        for (Cluster current: centroids) {
            workArray[index] = new DendrogramNode(current);
            index++;
        }

        //TODO: make PQueueSimilarity an inner class?
        //as it only makes sense relative to the work array

        //initialise pQueue
        //the initial size of the pQueue is size^2 and the maximum size is the double
        //TODO: worry about the space used by p-queue
        PriorityQueue<PQueueSimilarity> pQueue = new PriorityQueue<PQueueSimilarity>(2*size*size);
        for (int i=0; i<size-1; i++) {
            for (int j=i+1; j<size; j++) {
                double simValue = workArray[i].getCentroid().similarity(workArray[j].getCentroid());
                pQueue.offer(new PQueueSimilarity(simValue, i, j));
            }
        }
        //build dendrogram (look at notes)
        DendrogramNode parent = null;
        while (!pQueue.isEmpty()) {
            PQueueSimilarity sim = pQueue.poll();
            //'old similarities' are not removed when outdated
            //but simply skipped when encountered
            if (workArray[sim.getI()]==null || workArray[sim.getJ()]==null) {
                continue;
            }
            DendrogramNode nodeI = workArray[sim.getI()];
            DendrogramNode nodeJ = workArray[sim.getJ()];
            //create parent (centroid name not important)
            String name = combineName(nodeI, nodeJ);
            Cluster combined =
                    join(nodeI, nodeJ, name);
            parent = new DendrogramNode(combined);
            //if node i and j centroids are 'too' similar, throw away originals,
            if (sim.getValue()>joinSimThreshold) {
                if (log.isTraceEnabled()) {
                    log.trace("Combining node i = '"+nodeI.getName()+
                            "' and node j = '"+nodeJ.getName()+"'; similarity = "+
                            sim.getValue() + "; new name = " + name);
                }
            } else { //else add node i and j as children
                parent.addChild(nodeI);
                parent.addChild(nodeJ);
            }
            //delete 'old' nodes from work list
            workArray[sim.getI()] = null;
            workArray[sim.getJ()] = null;
            //add parent to worklist and add new similarities to p-queue
            workArray[size] = parent;
            for (int k=0; k<size; k++) {
                if (workArray[k]!=null) {
                    double simValue = workArray[k].getCentroid().similarity(parent.getCentroid());
                    pQueue.offer(new PQueueSimilarity(simValue, k, size));
                }
            }
            size++;
        }

        return new Dendrogram(parent);
    }

    /**
     * Combine the names of two clusters in a 'reasonable' way.
     * @param first centroid for the first cluster
     * @param second centroid for the second cluster
     * @return a new name for the combined cluster
     */
    private String combineName(Cluster first, Cluster second) {
        String name;
        if (first.getName().toLowerCase().equals(
                second.getName().toLowerCase())) {
            name = first.getName();
        } else {
            String semicolon = ";";
            name = first.getName();
            String[] names = first.getName().split(semicolon);
            String[] namesSecond = second.getName().split(semicolon);
            for (String nameFromSecondCentroid : namesSecond) {
                boolean nameFromSecondOk = true;
                for (String nameFromFirst : names) {
                    if (nameFromSecondCentroid.equals(nameFromFirst)) {
                        nameFromSecondOk = false;
                    }
                }
                if (nameFromSecondOk) {
                    name = name + semicolon + nameFromSecondCentroid;
                }
            }
            //should the combination be 'largest first'?
            //should the following two ifs be dropped?
            if (second.getExpectedSize()<.2*first.getExpectedSize()) {
                name = first.getName();
            }
            if (first.getExpectedSize()<.2*second.getExpectedSize()) {
                name = second.getName();
            }
        }
        return name;
    }

    /**
     * Join close clusters with same name; rename distant clusters with same name.
     * I.e. make sure that all clusters have different names.
     * @param clusters clusterset
     * @return clusterset without name conflicts
     */
    private ClusterSet resolveNameConflicts(ClusterSet clusters) {
        //todo update resolve name conflicts
        if (clusters ==null) {return null;}

        //get join same name similarity threshold
        double value;
        try {
            value = Double.parseDouble(conf.getString(JOIN_SIMILARITY_THRESHOLD_SAME_NAME_KEY));
        } catch (NumberFormatException e) {
            log.warn("ClusterMergerImpl.resolveNameConflicts The " +
                    "double property with key " + JOIN_SIMILARITY_THRESHOLD_SAME_NAME_KEY +
                    "could not be parsed.", e);
            value = .8;
        }
        double joinSimThresholdSameName =
                value;

        //centroid array used for the following loops
        Cluster[] clusterArray =
                clusters.toArray(new Cluster[clusters.size()]);

        //set for new joined clusters
        ClusterSet newClusterSet = new ClusterSet();

        int listSize = clusterArray.length;
        for (int i=0; i<listSize; i++) {
            Cluster first = clusterArray[i];

            for (int j=i+1; j<listSize; j++) {
                if (first == null) {break;}

                Cluster second = clusterArray[j];
                if (second == null) {continue;}

                // join the two clusters 'first' and 'second', if the names of the clusters
                // are the same and the two clusters are close, otherwise rename both
                if (first.getName().toLowerCase().equals(
                        second.getName().toLowerCase())) {

                    if (first.getCentroid().similarity(second.getCentroid())>joinSimThresholdSameName) {
                        //find a new name for the cluster
                        String name = first.getName().toLowerCase();
                        if (log.isTraceEnabled()) {
                            log.trace("Combining first = '"+first.getName()+
                                    "' and second = '"+second.getName()+"'; similarity = "+
                                    first.getCentroid().similarity(second.getCentroid())
                                    + "; new name = " + name);
                        }

                        //create new cluster based on the two known clusters
                        Cluster newCluster = join(first, second, name);

                        //add new centroid to the new centroid set
                        newClusterSet.add(newCluster);
                        //update first
                        first = newCluster;

                        //remove 'old' clusters from work array
                        clusterArray[i] = null;
                        clusterArray[j] = null;
                    } else {
                        //rename using 'top' coordinates
                        //TODO: think about rename

                        //get coordinates for first; sort descending
                        List<Map.Entry<String, Number>> coorFirst =
                                new ArrayList<Map.Entry<String, Number>>(first.getCentroid().getCoordinates().entrySet());
                        Collections.sort(coorFirst, Collections.reverseOrder(new CoordinateComparator()));
                        //get coordinates for second; sort descending
                        List<Map.Entry<String, Number>> coorSecond =
                                new ArrayList<Map.Entry<String, Number>>(second.getCentroid().getCoordinates().entrySet());
                        Collections.sort(coorSecond, Collections.reverseOrder(new CoordinateComparator()));
                        //add coordinate names to centroid names
                        int index = 0;
                        while (index < coorFirst.size() && index < coorSecond.size()
                                && first.getName().toLowerCase().equals(second.getName().toLowerCase())) {
                            if (!first.getName().toLowerCase().equals(coorFirst.get(index).getKey())) {
                                first.setName(first.getName().toLowerCase() + "; " + coorFirst.get(index).getKey());
                            }
                            if (!second.getName().toLowerCase().equals(coorSecond.get(index).getKey())) {
                                second.setName(second.getName().toLowerCase() + "; " + coorSecond.get(index).getKey());
                            }
                            index++;
                        }
                    }
                }
            }
        }
        //copy all the old clusters that survived to the new centroid set
        for (Cluster cluster: clusterArray) {
            if (cluster != null) {
                newClusterSet.add(cluster);
            }
        }

        return newClusterSet;
    }

    /**
     * Join the two centroids under the given name.
     * Or more accurately: Create centroid representing a cluster joined of the
     * two clusters represented by the given centroids (under the given name).
     * A new similarity threshold is calculated such that any document vector
     * that belongs to either of the clusters represented by the given
     * centroids will also belong to the cluster represented by the new
     * centroid.
     * @param first first centroid
     * @param second second centroid
     * @param name name for joined cluster
     * @return centroid representing joined cluster
     */
    public Cluster join(Cluster first, Cluster second, String name) {
        IncrementalCentroid newIncCentroid = new IncrementalCentroid(name);
        //TODO: should the centroids be weighted with expected size?
        newIncCentroid.addPoint(first.getCentroid());
        newIncCentroid.addPoint(second.getCentroid());
        Cluster combined = newIncCentroid.getCluster();

        combined.setExpectedSize(first.getExpectedSize() + second.getExpectedSize());
        //TODO: new similarity threshold is important - look at notes
        if (log.isTraceEnabled()) {
            log.trace("first.getSimilarityThreshold() = " + first.getSimilarityThreshold());
            log.trace("combined.getCentroid().similarity(first.getCentroid()) = "
                    + combined.getCentroid().similarity(first.getCentroid()));
            log.trace("second.getSimilarityThreshold() = " + second.getSimilarityThreshold());
            log.trace("combined.getCentroid().similarity(second.getCentroid()) = "
                    + combined.getCentroid().similarity(second.getCentroid()));
        }

        double simThrsAngle1 = Math.acos(first.getSimilarityThreshold());
        double simNewAngle1 = Math.acos(combined.getCentroid().similarity(first.getCentroid()));
        double simThrsAngle2 = Math.acos(second.getSimilarityThreshold());
        double simNewAngle2 = Math.acos(combined.getCentroid().similarity(second.getCentroid()));
        double maxAngle = Math.max(simThrsAngle1 + simNewAngle1,
                simThrsAngle2 + simNewAngle2);
        if (maxAngle>Math.PI/2) {
            combined.setSimilarityThreshold(0);
            log.trace("Similarity threshold set to 0.");
            //TODO: once we reach similarity threshold 0, there is no point in
            //TODO: continuing building the dendrogram - we might as well have a forest...
        } else {
            combined.setSimilarityThreshold(Math.cos(maxAngle));
            log.trace("Similarity threshold set to " + Math.cos(maxAngle) + ".");
        }

        return combined;
    }

    /**
     * Load and join centroid sets from all the local centroid builders.
     * @return set of all centroids from all local builders
     */
    private ClusterSet loadCentroids() {
        String centroidSetsPath = conf.getString(CLUSTER_SETS_PATH_KEY);
        File dir = new File(centroidSetsPath);
        if (dir.isDirectory()) {
            File[] centroidSetsFileList = dir.listFiles();
            ClusterSet resultClusterSet = new ClusterSet();
            for (File file: centroidSetsFileList) {
                ClusterSet set = ClusterSet.load(file);
                if (set != null) {
                    resultClusterSet.addAll(set);
                }
            }
            return resultClusterSet;
        } else {
            log.error("ClusterMergerImpl.loadCentroids not able to load " +
                    "centroids; centroidSetsPath = " + centroidSetsPath +
                    " not a directory; null is returned.");
            return null;
        }
    }

    /**
     * Expose the Merger as a remote service over rmi.
     * See dk.statsbiblioteket.summa.score.client.Client for implementation.
     * @throws RemoteException if failed to export remote interfaces
     */
    private void exportRemoteInterfaces() throws RemoteException {
        //TODO implement exportRemoteInterfaces
    }
}
