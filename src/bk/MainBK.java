package bk;

import filecheck.Constant;

import java.io.*;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by ChenTao on 2015/9/10.
 */
public class MainBK {

    // projects to be compared(base version is Master-AmQue)
    private static Set<String> toBeComparedProjects = new LinkedHashSet<>();

    // projects which have compared
    private static List<String> comparedProjects = new ArrayList<>();

    private static Map<String, String> projectDirMap = new LinkedHashMap<>();

    private static int fileCount = 0;

    // file MD5 map
    private static Map<String, Map<String, String>> allProjectMD5Map = new LinkedHashMap();

    // file version map
    private static Map<String, Map<String, Integer>> allProjectVersionMap = new LinkedHashMap();

    // files cross all project
    private static Set<String> allFiles = new TreeSet<>();

    private static Map<String, Integer> maxFileVersionMap = new LinkedHashMap();

    // maintain projectDirMap to determine which project will be checked
    static {
        projectDirMap.put(Constant.MASTER_PROJECT,           Constant.MASTER_DIR);
        projectDirMap.put(Constant.CloudExpress_PROJECT,     Constant.CloudExpress_DIR);
        projectDirMap.put(Constant.CMB_Cards_PROJECT,        Constant.CMB_Cards_DIR);
        projectDirMap.put(Constant.CTCF_PROJECT,             Constant.CTCF_DIR);
        projectDirMap.put(Constant.MASHANG_PROJECT,          Constant.MASHANG_DIR);
        projectDirMap.put(Constant.JIMU_PROJECT,             Constant.JIMU_DIR);
        projectDirMap.put(Constant.UCREDIT_PROJECT,          Constant.UCREDIT_DIR);
        projectDirMap.put(Constant.VCredit_PROJECT,          Constant.VCredit_DIR);
        projectDirMap.put(Constant.CMB_Retail_PROJECT,       Constant.CMB_Retail_DIR);
        projectDirMap.put(Constant.CreditEase_PROJECT,       Constant.CreditEase_DIR);

        toBeComparedProjects.addAll(projectDirMap.keySet());
        toBeComparedProjects.remove(Constant.MASTER_PROJECT);
    }

    // file count map, these files exist in Master-AmQue
    private static Map<String, Integer> filesCount = new LinkedHashMap<>();

    private static Map<String, Integer> filesInheritedFromMaster = new LinkedHashMap<>();

    private static Map<String, Integer> filesRemovedFromMaster = new LinkedHashMap<>();

    private static Map<String, Integer> filesAddedBasedOnMaster = new LinkedHashMap<>();
    
    private static Map<String, Integer> filesSameAsMaster = new LinkedHashMap<>();

    private static Map<String, String> filesSameAsMasterRate = new LinkedHashMap<>();

    private static Map<String, Map<String, Integer>> filesCountInAppCommon = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppControllers = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppDto = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppIoc = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppJobs = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppModels = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppService = new LinkedHashMap<>();
    private static Map<String, Map<String, Integer>> filesCountInAppViews = new LinkedHashMap<>();

    private static Map<String, Map<String, Integer>> filesInheritedFromMasterInAppCommon = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {

        // get file and its MD5 map for each project
        prepareFileMD5();

        // get file version for each project
        prepareVersionMap();

        // statistics based on version result
        statisticFiles();

        // output to file
        outputVersionFile(Constant.OUTPUT_VERSION_FILE_NAME);

        // output statistics file
        outputStatisticsFile(Constant.OUTPUT_STATICS_FILE_NAME);

    }

    private static void outputStatisticsFile(String outputStaticsFileName) throws FileNotFoundException {
        PrintWriter p = new PrintWriter(outputStaticsFileName);
        StringBuilder header = new StringBuilder(Constant.OUTPUT_HEADER_CATEGORY);
        for (String preparedProject : comparedProjects) {
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

    private static String formatOutput(String category, Map output) {
        StringBuilder sb = new StringBuilder(category);
        for (String preparedProject : comparedProjects) {
            sb.append(Constant.COMMA).append(output.get(preparedProject));
        }
        return sb.toString();
    }

    private static void statisticFiles() {

        // check file count for each project
        countFiles();

        // check files inherited from Master for each project
        checkFileInheritedFromMaster();

        // check newly added files based on Master
        checkNewFileBasedOnMaster();

        // check the same file as Master
        checkSameFileAsMaster();
    }

    private static void checkSameFileAsMaster() {

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

    private static void countFiles() {
        for (String projectName : allProjectMD5Map.keySet()) {
            filesCount.put(projectName, allProjectMD5Map.get(projectName).size());

            int appCommonFileCount = 0;
            Map<String, Integer> appCommonFileCountMap = new HashMap<>();
            for (String fileName : allProjectMD5Map.get(projectName).keySet()) {
                if (fileName.contains(Constant.APP_COMMON)) {
                    appCommonFileCount++;
                }
            }
            appCommonFileCountMap.put(Constant.APP_COMMON, appCommonFileCount);
            filesCountInAppCommon.put(projectName, appCommonFileCountMap);
        }
        System.out.println("File count in each project:" + filesCount);
        System.out.println("app/common File count in each project:" + filesCountInAppCommon);
    }

    private static void checkNewFileBasedOnMaster() {

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

    private static void checkFileInheritedFromMaster() {

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

    private static void outputVersionFile(String outputFileName) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter p = new PrintWriter(outputFileName);
        StringBuilder header = new StringBuilder(Constant.OUTPUT_HEADER_FILE_NAME);
        for (String preparedProject : comparedProjects) {
            header.append(Constant.COMMA).append(preparedProject);
        }
        header.append(Constant.COMMA).append(Constant.OUTPUT_HEADER_MAX_VERSION);
        p.println(header.toString());

        for (String fileName : allFiles) {
            StringBuilder content = new StringBuilder(fileName);
            for (String preparedProject : comparedProjects) {
                content.append(Constant.COMMA).append(allProjectVersionMap.get(preparedProject).get(fileName));
            }
            content.append(Constant.COMMA).append(maxFileVersionMap.get(fileName));
            p.println(content.toString());
        }
        p.close();
    }

    private static void prepareVersionMap() {

        // prepare Master-AmQue file version
        Map<String, Integer> versionMap = new HashMap<>();
        Map<String, String> md5Map= allProjectMD5Map.get(Constant.MASTER_PROJECT);

        // compare to global files, if file exists, then set 1, if not, then set 0
        for (String fileName : allFiles) {
            if (md5Map.get(fileName) == null) {
                versionMap.put(fileName, 0);
                maxFileVersionMap.put(fileName, 0);
            } else {
                versionMap.put(fileName, 1);
                maxFileVersionMap.put(fileName, 1);
            }

        }
        // cache Master-AmQue file version map
        allProjectVersionMap.put(Constant.MASTER_PROJECT, versionMap);
        // set Master-AmQue as prepared
        comparedProjects.add(Constant.MASTER_PROJECT);

        // for all other project, compare the files with the file which has prepared
        // if compared file is the same as the file in some project, then set the same file version
        // if cannot find same file, then plus 1 based on the max file version
        for (String projectName : toBeComparedProjects) {
            versionMap = new HashMap<>();
            md5Map = allProjectMD5Map.get(projectName);
            for (String fileName : allFiles) {
                if (md5Map.get(fileName) == null) {
                    versionMap.put(fileName, 0);
                } else {
                    int maxVersion = 0;
                    boolean isHit = false;
                    for (String preparedProject : comparedProjects) {
                        int toBeComparedVersion = allProjectVersionMap.get(preparedProject).get(fileName);
                        if (maxVersion < toBeComparedVersion) {
                            maxVersion = toBeComparedVersion;
                        }
                        if (md5Map.get(fileName).equals(allProjectMD5Map.get(preparedProject).get(fileName))) {
                            versionMap.put(fileName, toBeComparedVersion);
                            isHit = true;
                            break;
                        }
                    }
                    if (!isHit) {
                        maxVersion = maxVersion + 1;
                        versionMap.put(fileName, maxVersion);
                    }
                    if (maxFileVersionMap.get(fileName) < maxVersion) {
                        maxFileVersionMap.put(fileName, maxVersion);
                    }
                }
            }
            allProjectVersionMap.put(projectName, versionMap);
            comparedProjects.add(projectName);
        }
    }

    private static void prepareFileMD5() throws FileNotFoundException {

        File dir;
        Map<String, String> fileMap;
        for (Map.Entry<String, String> entry : projectDirMap.entrySet()) {
            dir = new File(entry.getValue());
            fileMap = new HashMap<>();
            exploreFile(dir, fileMap);
            allProjectMD5Map.put(entry.getKey(), fileMap);
        }
    }

    public static void exploreFile(File file, Map<String, String> fileMap) throws FileNotFoundException {
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if (!isValidJavaFile(f)) continue;
                cacheFileMD5(fileMap, f);
            } else {
                exploreFile(f, fileMap);
            }
        }
    }

    private static boolean isValidJavaFile(File file) {
        String fileName = file.getName();
        boolean isJavaFile = fileName.endsWith(".java");
        boolean isTestFile = fileName.contains("Test");
        return isJavaFile && !isTestFile;
    }

    private static void cacheFileMD5(Map<String, String> fileMap, File f) throws FileNotFoundException {
        String fileNameFull = f.getAbsolutePath();
        String fileNamePackage = fileNameFull.substring(fileNameFull.indexOf("-AmQue") + "-AmQue".length() + 1);
        fileMap.put(fileNamePackage, getMd5ByFile(f));
        allFiles.add(fileNamePackage);
    }

    public static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
}