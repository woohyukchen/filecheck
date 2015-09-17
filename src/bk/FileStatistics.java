package bk;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ChenTao on 2015/9/14.
 */
public class FileStatistics {

    // file MD5 map
    private Map<String, Map<String, String>> allProjectMD5Map = new LinkedHashMap();

    // file version map
    private Map<String, Map<String, Integer>> allProjectVersionMap = new LinkedHashMap();

    // file count map, these files exist in Master-AmQue
    private Map<String, Integer> filesCount = new LinkedHashMap<>();

    private Map<String, Integer> filesInheritedFromMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesRemovedFromMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesAddedBasedOnMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesSameAsMaster = new LinkedHashMap<>();

    private Map<String, String> filesSameAsMasterRate = new LinkedHashMap<>();

    private Map<String, Map<String, Integer>> filesCountInAppCommon = new LinkedHashMap<>();

    private static Set<String> packagePaths = new LinkedHashSet<>();

    static {
        packagePaths.add(Constant.APP_COMMON);
        packagePaths.add(Constant.APP_CONTROLLERS);
        packagePaths.add(Constant.APP_DTO);
        packagePaths.add(Constant.APP_IOC);
        packagePaths.add(Constant.APP_JOBS);
        packagePaths.add(Constant.APP_MODELS);
        packagePaths.add(Constant.APP_SERVICE);
    }
    public FileStatistics(Map<String, Map<String, String>> allProjectMD5Map, Map<String, Map<String, Integer>> allProjectVersionMap) {
        this.allProjectMD5Map = allProjectMD5Map;
        this.allProjectVersionMap = allProjectVersionMap;
    }

    public void statisticFiles() throws FileNotFoundException {

        // check file count for each project
        countFiles();

        // check files inherited from Master for each project
        checkFileInheritedFromMaster();

        // check newly added files based on Master
        checkNewFileBasedOnMaster();

        // check the same file as Master
        checkSameFileAsMaster();

        outputStatisticsFile(Constant.OUTPUT_STATICS_FILE_NAME);
    }

    public void outputStatisticsFile(String outputStaticsFileName) throws FileNotFoundException {
        PrintWriter p = new PrintWriter(outputStaticsFileName);
        StringBuilder header = new StringBuilder(Constant.OUTPUT_HEADER_CATEGORY);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            header.append(Constant.COMMA).append(preparedProject);
        }
        p.println(header.toString());

        p.println(formatOutput("File Count", filesCount));
        p.println(formatOutput("File Exists In Both Master And Project", filesInheritedFromMaster));
        p.println(formatOutput("File Exists In Master But Not In Project", filesRemovedFromMaster));
        p.println(formatOutput("File Newly added In Project", filesAddedBasedOnMaster));
        p.println(formatOutput("File Same As Master", filesSameAsMaster));
        p.println(formatOutput("Rate Of Same File As Master", filesSameAsMasterRate));

        p.close();
    }


    private String formatOutput(String category, Map output) {
        StringBuilder sb = new StringBuilder(category);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            sb.append(Constant.COMMA).append(output.get(preparedProject));
        }
        return sb.toString();
    }

    private void countFiles() {
        for (String projectName : allProjectMD5Map.keySet()) {
            // count all files
            filesCount.put(projectName, allProjectMD5Map.get(projectName).size());

            // count files for each package
            filesCountInAppCommon.put(projectName, countFilesForPackage(projectName, packagePaths));
        }
        System.out.println("File count in each project:" + filesCount);
        System.out.println("File count in each project:" + filesCountInAppCommon);
    }

    private Map<String, Integer> countFilesForPackage(String projectName, Set<String> packagePaths) {

        Map<String, Integer> fileCountMap = new LinkedHashMap<>();
        for (String packagePath : packagePaths) {
            int fileCount = 0;
            for (String fileName : allProjectMD5Map.get(projectName).keySet()) {
                if (fileName.contains(packagePath)) {
                    fileCount++;
                }
            }
            fileCountMap.put(packagePath, fileCount);
        }
        return fileCountMap;
    }

    private void checkFileInheritedFromMaster() {

        for (String projectName : allProjectMD5Map.keySet()) {
            int inheritFileCount = 0;
            int removedFileCount = 0;
            for (String fileName : allProjectMD5Map.get(Constant.MASTER_PROJECT).keySet()) {
                if (allProjectMD5Map.get(projectName).get(fileName) != null) {
                    inheritFileCount++;
                } else {
                    removedFileCount++;
                }

            }
            filesInheritedFromMaster.put(projectName, inheritFileCount);
            filesRemovedFromMaster.put(projectName, removedFileCount);
        }
        System.out.println("Files exist in Master and in individual project:" + filesInheritedFromMaster);
        System.out.println("Files exist in Master but not in individual project:" + filesRemovedFromMaster);
    }

    private void checkNewFileBasedOnMaster() {

        for (String projectName : allProjectMD5Map.keySet()) {
            int newFileCount = 0;
            for (String fileName : allProjectMD5Map.get(projectName).keySet()) {
                if (allProjectMD5Map.get(Constant.MASTER_PROJECT).get(fileName) == null) {
                    newFileCount++;
                }

            }
            filesAddedBasedOnMaster.put(projectName, newFileCount);
        }
        System.out.println("Files newly added based on Master:" + filesAddedBasedOnMaster);
    }

    private void checkSameFileAsMaster() {

        for (String projectName : allProjectVersionMap.keySet()) {
            int sameFileCount = 0;
            for (Map.Entry<String, Integer> versionMap : allProjectVersionMap.get(Constant.MASTER_PROJECT).entrySet()) {
                if (versionMap.getValue() == 0) continue;
                if (versionMap.getValue() == allProjectVersionMap.get(projectName).get(versionMap.getKey())) {
                    sameFileCount++;
                }
            }
            filesSameAsMaster.put(projectName, sameFileCount);
        }
        System.out.println("Files same as Master:" + filesSameAsMaster);

        int masterFileCount = filesSameAsMaster.get(Constant.MASTER_PROJECT);
        for (Map.Entry<String, Integer> fileCountMap : filesSameAsMaster.entrySet()) {
            double rate = (double) Math.round(fileCountMap.getValue()*100)/masterFileCount;
            DecimalFormat df = new DecimalFormat("0.00");

            filesSameAsMasterRate.put(fileCountMap.getKey(), df.format(rate));
        }
        System.out.println("Files same rate as Master:" + filesSameAsMasterRate);
    }
}
