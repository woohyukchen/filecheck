package bk;

import filecheck.Constant;
import filecheck.FileAttributeDto;

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
public class FileStatisticsBK {

    public static final String PACKAGE_NAME = "Package Name";
    // file MD5 map
    private Map<String, Map<String, String>> allProjectMD5Map = new LinkedHashMap();

    // file version map
    private Map<String, Map<String, Integer>> allProjectVersionMap = new LinkedHashMap();

    private Map<String, Map<String, FileAttributeDto>> analyzedFileMap = new LinkedHashMap<>();

    // projectName - category - packagePath - fileCount
    private Map<String, Map<String, Map<String, String>>> fileCountMap = new LinkedHashMap<>();

    // file count map, these files exist in Master-AmQue
    private Map<String, Integer> filesCount = new LinkedHashMap<>();

    private Map<String, Integer> filesInheritedFromMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesRemovedFromMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesAddedBasedOnMaster = new LinkedHashMap<>();

    private Map<String, Integer> filesSameAsMaster = new LinkedHashMap<>();

    private Map<String, String> filesSameAsMasterRate = new LinkedHashMap<>();

    private Map<String, Map<String, Integer>> filesCountInAppCommon = new LinkedHashMap<>();

    private static Set<String> packagePaths = new LinkedHashSet<>();

    private static Set<String> categories = new LinkedHashSet<>();

    static {
        packagePaths.add(filecheck.Constant.APP);
        packagePaths.add(filecheck.Constant.APP_COMMON);
        packagePaths.add(filecheck.Constant.APP_CONTROLLERS);
        packagePaths.add(filecheck.Constant.APP_DTO);
        packagePaths.add(filecheck.Constant.APP_IOC);
        packagePaths.add(filecheck.Constant.APP_JOBS);
        packagePaths.add(filecheck.Constant.APP_MODELS);
        packagePaths.add(filecheck.Constant.APP_SERVICE);

        categories.add(filecheck.Constant.FILE_COUNT);
        categories.add(filecheck.Constant.FILE_EXISTS_IN_MASTER);
        categories.add(filecheck.Constant.FILE_EXISTS_IN_MASTER_BUT_NOT_IN_PROJECT);
        categories.add(filecheck.Constant.FILE_NEWLY_ADDED_IN_PROJECT);
        categories.add(filecheck.Constant.FILE_SAME_AS_MASTER);
        categories.add(filecheck.Constant.RATE_OF_SAME_FILE_AS_MASTER);
    }
    public FileStatisticsBK(Map<String, Map<String, String>> allProjectMD5Map, Map<String, Map<String, Integer>> allProjectVersionMap) {
        this.allProjectMD5Map = allProjectMD5Map;
        this.allProjectVersionMap = allProjectVersionMap;
    }

    public void statisticFiles() throws FileNotFoundException {

        // analyze file and form these information into FileAttributeDto
        analyzeFiles();

        doFileStatistics();
        // check file count for each project
        countFiles();

        // check files inherited from Master for each project
        checkFileInheritedFromMaster();

        // check newly added files based on Master
        checkNewFileBasedOnMaster();

        // check the same file as Master
        checkSameFileAsMaster();

//        outputStatisticsFile(Constant.OUTPUT_STATICS_FILE_NAME);

//        output("D:\\source\\fileAnalysis.csv");

        outputStatistics(filecheck.Constant.FILE_ANALYSIS_STATISTICS_CSV);
    }

    private void analyzeFiles() throws FileNotFoundException {

        Map<String, Integer> masterFileVersionMap = allProjectVersionMap.get(filecheck.Constant.MASTER_PROJECT);
        for (Map.Entry<String, Map<String, Integer>> projectVersionEntry : allProjectVersionMap.entrySet()) {

            String projectName = projectVersionEntry.getKey();
            Map<String, Integer> fileVersionMap = projectVersionEntry.getValue();
            Map<String, String> fileMD5Map = allProjectMD5Map.get(projectName);
            FileAttributeDto fad;
            Map<String, FileAttributeDto> fileMap = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> fileVersionEntry : fileVersionMap.entrySet()) {

                String fileName = fileVersionEntry.getKey();
                Integer fileVersion = fileVersionEntry.getValue();
                Integer fileVersionInMaster = masterFileVersionMap.get(fileName);

                fad = new FileAttributeDto();
                fad.setFileName(fileName);
                fad.setFileMD5(fileMD5Map.get(fileName));
                fad.setFileVersion(fileVersion);
                fad.setIsExistsInMaster(fileVersionInMaster != 0 && fileVersion != 0);
                fad.setIsNewlyAdded(fileVersionInMaster == 0 && fileVersion != 0);
                fad.setIsRemovedFromMaster(fileVersionInMaster != 0 && fileVersion == 0);
                fad.setIsSameAsMaster(fileVersionInMaster != 0 && fileVersion == fileVersionInMaster);

                String filePackagePath = fileName.substring(0, fileName.indexOf("\\", 4));
                fad.setPackagePath(filePackagePath);
                packagePaths.add(filePackagePath);

                fileMap.put(fileName, fad);
            }
            analyzedFileMap.put(projectName, fileMap);
        }

    }

    private void doFileStatistics() {

        for (Map.Entry<String, Map<String, FileAttributeDto>> allFadEntry : analyzedFileMap.entrySet()) {
            Map<String, FileAttributeDto> fileMap = allFadEntry.getValue();
            Map<String, Map<String, String>> categoryFileCountMap = new LinkedHashMap<>();
            for (String category : categories) {
                Map<String, String> fileInPackageMap = new LinkedHashMap<>();
                for (String packagePath : packagePaths) {
                    switch (category) {
                        case filecheck.Constant.FILE_COUNT:
                            fileInPackageMap.put(packagePath, Integer.toString(getFileCount(fileMap, packagePath)));
                            break;
                        case filecheck.Constant.FILE_EXISTS_IN_MASTER:
                            fileInPackageMap.put(packagePath, Integer.toString(getExistsInMasterFileCount(fileMap, packagePath)));
                            break;
                        case filecheck.Constant.FILE_EXISTS_IN_MASTER_BUT_NOT_IN_PROJECT:
                            fileInPackageMap.put(packagePath, Integer.toString(getRemovedFromMasterFileCount(fileMap, packagePath)));
                            break;
                        case filecheck.Constant.FILE_NEWLY_ADDED_IN_PROJECT:
                            fileInPackageMap.put(packagePath, Integer.toString(getNewlyAddFileCount(fileMap, packagePath)));
                            break;
                        case filecheck.Constant.FILE_SAME_AS_MASTER:
                            int sameFileCount = getSameAsMasterFileCount(fileMap, packagePath);
                            fileInPackageMap.put(packagePath, Integer.toString(sameFileCount));
                            break;
                        case filecheck.Constant.RATE_OF_SAME_FILE_AS_MASTER:
                            if (Integer.parseInt(fileCountMap.get(filecheck.Constant.MASTER_PROJECT).get(filecheck.Constant.FILE_COUNT).get(packagePath)) == 0) {
                                fileInPackageMap.put(packagePath, "0");
                            } else {
                                if (filecheck.Constant.MASTER_PROJECT.equals(allFadEntry.getKey())) {
                                    fileInPackageMap.put(packagePath, "100");
                                } else {
                                    fileInPackageMap.put(packagePath, getSameAsMasterFileRate(categoryFileCountMap.get(filecheck.Constant.FILE_SAME_AS_MASTER).get(packagePath), fileCountMap.get(filecheck.Constant.MASTER_PROJECT).get(filecheck.Constant.FILE_COUNT).get(packagePath)));
                                }
                            }
                            break;
                    }
                }
                categoryFileCountMap.put(category, fileInPackageMap);
                fileCountMap.put(allFadEntry.getKey(), categoryFileCountMap);
            }
        }
    }


    private void output(String outputFileName) throws FileNotFoundException {
        PrintWriter p = new PrintWriter(outputFileName);
        StringBuilder header = new StringBuilder(filecheck.Constant.OUTPUT_HEADER_CATEGORY);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            header.append(filecheck.Constant.COMMA).append(preparedProject);
        }
        p.println(header.toString());

        String masterFileCount = null;
        for (String category : categories) {
            StringBuilder sb = new StringBuilder(category);
            for (String projectName : allProjectMD5Map.keySet()) {
                String count = null;
                switch (category) {
                    case filecheck.Constant.FILE_COUNT:
                        count = Integer.toString(getFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        if (filecheck.Constant.MASTER_PROJECT.equals(projectName)) {
                            masterFileCount = count;
                        }
                        break;
                    case filecheck.Constant.FILE_EXISTS_IN_MASTER:
                        count = Integer.toString(getExistsInMasterFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        break;
                    case filecheck.Constant.FILE_EXISTS_IN_MASTER_BUT_NOT_IN_PROJECT:
                        count = Integer.toString(getRemovedFromMasterFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        break;
                    case filecheck.Constant.FILE_NEWLY_ADDED_IN_PROJECT:
                        count = Integer.toString(getNewlyAddFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        break;
                    case filecheck.Constant.FILE_SAME_AS_MASTER:
                        count = Integer.toString(getSameAsMasterFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        break;
                    case filecheck.Constant.RATE_OF_SAME_FILE_AS_MASTER:
                        String sameFileCount = Integer.toString(getSameAsMasterFileCount(analyzedFileMap.get(projectName), filecheck.Constant.APP));
                        count = getSameAsMasterFileRate(sameFileCount, masterFileCount);
                        break;

                }
                sb.append(filecheck.Constant.COMMA).append(count);
            }
            p.println(sb.toString());
        }

        p.close();
    }

    private void outputStatistics(String outputFileName) throws FileNotFoundException {

        PrintWriter p = new PrintWriter(outputFileName);
        StringBuilder header = new StringBuilder(filecheck.Constant.OUTPUT_HEADER_CATEGORY);
        header.append(filecheck.Constant.COMMA).append(PACKAGE_NAME);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            header.append(filecheck.Constant.COMMA).append(preparedProject);
        }
        p.println(header.toString());

        for (String category : categories) {
            for (String packagePath : packagePaths) {
                StringBuilder sb = new StringBuilder(category);
                sb.append(filecheck.Constant.COMMA).append(packagePath);
                for (String preparedProject : allProjectMD5Map.keySet()) {
                    sb.append(filecheck.Constant.COMMA).append(fileCountMap.get(preparedProject).get(category).get(packagePath));
                }
                p.println(sb.toString());
            }
        }

        p.close();
    }



    public int getFileCount(Map<String, FileAttributeDto> fileMap, String packagePath) {
        int fileCount = 0;
        for (FileAttributeDto fad : fileMap.values()) {
            if (fad.getFileVersion() != 0 && fad.getFileName().contains(packagePath)) {
                fileCount++;
            }
        }
        return fileCount;
    }

    public int getExistsInMasterFileCount(Map<String, FileAttributeDto> fileMap, String packagePath) {
        int fileCount = 0;
        for (FileAttributeDto fad : fileMap.values()) {
            if (fad.isExistsInMaster() && fad.getFileName().contains(packagePath)) {
                fileCount++;
            }
        }
        return fileCount;
    }

    public int getRemovedFromMasterFileCount(Map<String, FileAttributeDto> fileMap, String packagePath) {
        int fileCount = 0;
        for (FileAttributeDto fad : fileMap.values()) {
            if (fad.isRemovedFromMaster() && fad.getFileName().contains(packagePath)) {
                fileCount++;
            }
        }
        return fileCount;
    }

    public int getNewlyAddFileCount(Map<String, FileAttributeDto> fileMap, String packagePath) {
        int fileCount = 0;
        for (FileAttributeDto fad : fileMap.values()) {
            if (fad.isNewlyAdded() && fad.getFileName().contains(packagePath)) {
                fileCount++;
            }
        }
        return fileCount;
    }

    public int getSameAsMasterFileCount(Map<String, FileAttributeDto> fileMap, String packagePath) {
        int fileCount = 0;
        for (FileAttributeDto fad : fileMap.values()) {
            if (fad.isSameAsMaster() && fad.getFileName().contains(packagePath)) {
                fileCount++;
            }
        }
        return fileCount;
    }

    public String getSameAsMasterFileRate(String sameFileCount, String masterFileCountInPackagePath) {

        double rate = (double) Math.round(Integer.parseInt(sameFileCount)*100)/Integer.parseInt(masterFileCountInPackagePath);
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(rate);
    }

    public void outputStatisticsFile(String outputStaticsFileName) throws FileNotFoundException {
        PrintWriter p = new PrintWriter(outputStaticsFileName);
        StringBuilder header = new StringBuilder(filecheck.Constant.OUTPUT_HEADER_CATEGORY);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            header.append(filecheck.Constant.COMMA).append(preparedProject);
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
            sb.append(filecheck.Constant.COMMA).append(output.get(preparedProject));
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
            for (String fileName : allProjectMD5Map.get(filecheck.Constant.MASTER_PROJECT).keySet()) {
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
                if (allProjectMD5Map.get(filecheck.Constant.MASTER_PROJECT).get(fileName) == null) {
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
            for (Map.Entry<String, Integer> versionMap : allProjectVersionMap.get(filecheck.Constant.MASTER_PROJECT).entrySet()) {
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
