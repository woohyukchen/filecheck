package bk;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ChenTao on 2015/9/14.
 */
public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        Map<String, String> projectDirMap = new LinkedHashMap<>();
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

        // file version check
        FileCheck fc = new FileCheck(projectDirMap);
        fc.checkFile();

        // statistics based on version result
        FileStatistics fs = new FileStatistics(fc.allProjectMD5Map, fc.allProjectVersionMap);
        fs.statisticFiles();
    }
}
