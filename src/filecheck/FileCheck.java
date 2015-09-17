package filecheck;

import java.io.*;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.*;

/**
 * Created by ChenTao on 2015/9/14.
 */
public class FileCheck {

    // projects to be compared(base version is Master-AmQue)
    public Set<String> toBeComparedProjects = new LinkedHashSet<>();

    // projects which have compared
    public List<String> comparedProjects = new ArrayList<>();

    public Map<String, String> projectDirMap = new LinkedHashMap<>();

    // file MD5 map
    public Map<String, Map<String, String>> allProjectMD5Map = new LinkedHashMap();

    // file version map
    public Map<String, Map<String, Integer>> allProjectVersionMap = new LinkedHashMap();

    // files cross all project
    public static Set<String> allFiles = new TreeSet<>();

    public Map<String, Integer> maxFileVersionMap = new LinkedHashMap();

    public FileCheck(Map<String, String> projectDirMap) {
        this.projectDirMap = projectDirMap;

        toBeComparedProjects.addAll(projectDirMap.keySet());
        toBeComparedProjects.remove(Constant.MASTER_PROJECT);
    }

    public void checkFile() throws FileNotFoundException {

        prepareFileMD5();

        prepareVersionMap();

        outputVersionFile(Constant.OUTPUT_VERSION_FILE_NAME);
    }

    public void prepareFileMD5() throws FileNotFoundException {

        File dir;
        Map<String, String> fileMap;
        for (Map.Entry<String, String> entry : projectDirMap.entrySet()) {
            dir = new File(entry.getValue());
            fileMap = new HashMap<>();
            exploreFile(dir, fileMap);
            allProjectMD5Map.put(entry.getKey(), fileMap);
        }
    }

    public void prepareVersionMap() {

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

    public void outputVersionFile(String outputFileName) throws FileNotFoundException {
        PrintWriter p = new PrintWriter(outputFileName);
        StringBuilder header = new StringBuilder(Constant.OUTPUT_HEADER_FILE_NAME);
        for (String preparedProject : allProjectVersionMap.keySet()) {
            header.append(Constant.COMMA).append(preparedProject);
        }
        header.append(Constant.COMMA).append(Constant.OUTPUT_HEADER_MAX_VERSION);
        p.println(header.toString());

        for (String fileName : allFiles) {
            StringBuilder content = new StringBuilder(fileName);
            for (String preparedProject : allProjectVersionMap.keySet()) {
                content.append(Constant.COMMA).append(allProjectVersionMap.get(preparedProject).get(fileName));
            }
            content.append(Constant.COMMA).append(maxFileVersionMap.get(fileName));
            p.println(content.toString());
        }
        p.close();
    }

    private void exploreFile(File file, Map<String, String> fileMap) throws FileNotFoundException {
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

    private boolean isValidJavaFile(File file) {
        String fileName = file.getName();
        boolean isJavaFile = fileName.endsWith(".java");
        boolean isTestFile = fileName.contains("Test");
        return isJavaFile && !isTestFile;
    }

    private void cacheFileMD5(Map<String, String> fileMap, File f) throws FileNotFoundException {
        String fileNameFull = f.getAbsolutePath();
        String fileNamePackage = fileNameFull.substring(fileNameFull.indexOf("-AmQue") + "-AmQue".length() + 1);
        fileMap.put(fileNamePackage, getFileMD5(f));
        allFiles.add(fileNamePackage);
    }

    public String getFileMD5(File file) throws FileNotFoundException {
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
