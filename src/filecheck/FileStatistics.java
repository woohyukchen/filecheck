package filecheck;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by ChenTao on 2015/9/14.
 */
public class FileStatistics {

    // file MD5 map
    private Map<String, Map<String, String>> allProjectMD5Map = new LinkedHashMap();

    // file version map
    private Map<String, Map<String, Integer>> allProjectVersionMap = new LinkedHashMap();

    private Map<String, Map<String, FileAttributeDto>> analyzedFileMap = new LinkedHashMap<>();

    // projectName - category - packagePath - fileCount
    private Map<String, Map<String, Map<String, String>>> fileCountMap = new LinkedHashMap<>();

    private static Set<String> packagePaths = new LinkedHashSet<>();

    private static Set<String> categories = new LinkedHashSet<>();

    static {
        packagePaths.add(Constant.APP);
        packagePaths.add(Constant.APP_COMMON);
        packagePaths.add(Constant.APP_CONTROLLERS);
        packagePaths.add(Constant.APP_DTO);
        packagePaths.add(Constant.APP_IOC);
        packagePaths.add(Constant.APP_JOBS);
        packagePaths.add(Constant.APP_MODELS);
        packagePaths.add(Constant.APP_SERVICE);

        categories.add(Constant.FILE_COUNT);
        categories.add(Constant.FILE_EXISTS_IN_MASTER);
        categories.add(Constant.FILE_EXISTS_IN_MASTER_BUT_NOT_IN_PROJECT);
        categories.add(Constant.FILE_NEWLY_ADDED_IN_PROJECT);
        categories.add(Constant.FILE_SAME_AS_MASTER);
        categories.add(Constant.RATE_OF_SAME_FILE_AS_MASTER);
    }
    public FileStatistics(Map<String, Map<String, String>> allProjectMD5Map, Map<String, Map<String, Integer>> allProjectVersionMap) {
        this.allProjectMD5Map = allProjectMD5Map;
        this.allProjectVersionMap = allProjectVersionMap;
    }

    public void statisticFiles() throws FileNotFoundException {

        // analyze file and form these information into FileAttributeDto
        analyzeFiles();

        doFileStatistics();

        outputStatistics(Constant.FILE_ANALYSIS_STATISTICS_CSV);
    }

    private void analyzeFiles() throws FileNotFoundException {

        Map<String, Integer> masterFileVersionMap = allProjectVersionMap.get(Constant.MASTER_PROJECT);
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
                        case Constant.FILE_COUNT:
                            fileInPackageMap.put(packagePath, Integer.toString(getFileCount(fileMap, packagePath)));
                            break;
                        case Constant.FILE_EXISTS_IN_MASTER:
                            fileInPackageMap.put(packagePath, Integer.toString(getExistsInMasterFileCount(fileMap, packagePath)));
                            break;
                        case Constant.FILE_EXISTS_IN_MASTER_BUT_NOT_IN_PROJECT:
                            fileInPackageMap.put(packagePath, Integer.toString(getRemovedFromMasterFileCount(fileMap, packagePath)));
                            break;
                        case Constant.FILE_NEWLY_ADDED_IN_PROJECT:
                            fileInPackageMap.put(packagePath, Integer.toString(getNewlyAddFileCount(fileMap, packagePath)));
                            break;
                        case Constant.FILE_SAME_AS_MASTER:
                            int sameFileCount = getSameAsMasterFileCount(fileMap, packagePath);
                            fileInPackageMap.put(packagePath, Integer.toString(sameFileCount));
                            break;
                        case Constant.RATE_OF_SAME_FILE_AS_MASTER:
                            if (Integer.parseInt(fileCountMap.get(Constant.MASTER_PROJECT).get(Constant.FILE_COUNT).get(packagePath)) == 0) {
                                fileInPackageMap.put(packagePath, "0");
                            } else {
                                if (Constant.MASTER_PROJECT.equals(allFadEntry.getKey())) {
                                    fileInPackageMap.put(packagePath, "100");
                                } else {
                                    fileInPackageMap.put(packagePath, getSameAsMasterFileRate(categoryFileCountMap.get(Constant.FILE_SAME_AS_MASTER).get(packagePath), fileCountMap.get(Constant.MASTER_PROJECT).get(Constant.FILE_COUNT).get(packagePath)));
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

    private void outputStatistics(String outputFileName) throws FileNotFoundException {

        PrintWriter p = new PrintWriter(outputFileName);
        StringBuilder header = new StringBuilder(Constant.OUTPUT_HEADER_CATEGORY);
        header.append(Constant.COMMA).append(Constant.PACKAGE_NAME);
        for (String preparedProject : allProjectMD5Map.keySet()) {
            header.append(Constant.COMMA).append(preparedProject);
        }
        p.println(header.toString());

        for (String category : categories) {
            for (String packagePath : packagePaths) {
                StringBuilder sb = new StringBuilder(category);
                sb.append(Constant.COMMA).append(packagePath);
                for (String preparedProject : allProjectMD5Map.keySet()) {
                    sb.append(Constant.COMMA).append(fileCountMap.get(preparedProject).get(category).get(packagePath));
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
}
