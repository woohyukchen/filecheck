package filecheck;

/**
 * Created by ChenTao on 2015/9/15.
 */
public class FileAttributeDto {

    private String fileName;

    private String fileMD5;

    private Integer fileVersion;

    private String packagePath;

    private boolean isExistsInMaster;

    private boolean isRemovedFromMaster;

    private boolean isNewlyAdded;

    private boolean isSameAsMaster;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    public Integer getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(Integer fileVersion) {
        this.fileVersion = fileVersion;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public boolean isExistsInMaster() {
        return isExistsInMaster;
    }

    public void setIsExistsInMaster(boolean isExistsInMaster) {
        this.isExistsInMaster = isExistsInMaster;
    }

    public boolean isRemovedFromMaster() {

        return isRemovedFromMaster;
    }

    public void setIsRemovedFromMaster(boolean isRemovedFromMaster) {
        this.isRemovedFromMaster = isRemovedFromMaster;
    }

    public boolean isNewlyAdded() {
        return isNewlyAdded;
    }

    public void setIsNewlyAdded(boolean isNewlyAdded) {
        this.isNewlyAdded = isNewlyAdded;
    }

    public boolean isSameAsMaster() {
        return isSameAsMaster;
    }

    public void setIsSameAsMaster(boolean isSameAsMaster) {
        this.isSameAsMaster = isSameAsMaster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileAttributeDto that = (FileAttributeDto) o;

        if (isExistsInMaster != that.isExistsInMaster) return false;
        if (isRemovedFromMaster != that.isRemovedFromMaster) return false;
        if (isNewlyAdded != that.isNewlyAdded) return false;
        if (isSameAsMaster != that.isSameAsMaster) return false;
        if (!fileName.equals(that.fileName)) return false;
        if (fileMD5 != null ? !fileMD5.equals(that.fileMD5) : that.fileMD5 != null) return false;
        if (fileVersion != null ? !fileVersion.equals(that.fileVersion) : that.fileVersion != null) return false;
        return !(packagePath != null ? !packagePath.equals(that.packagePath) : that.packagePath != null);

    }

    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + (fileMD5 != null ? fileMD5.hashCode() : 0);
        result = 31 * result + (fileVersion != null ? fileVersion.hashCode() : 0);
        result = 31 * result + (packagePath != null ? packagePath.hashCode() : 0);
        result = 31 * result + (isExistsInMaster ? 1 : 0);
        result = 31 * result + (isRemovedFromMaster ? 1 : 0);
        result = 31 * result + (isNewlyAdded ? 1 : 0);
        result = 31 * result + (isSameAsMaster ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileAttributeDto{" +
                "  fileName='" + fileName + '\'' +
                ", fileMD5='" + fileMD5 + '\'' +
                ", fileVersion=" + fileVersion +
                ", packagePath='" + packagePath + '\'' +
                ", isExistsInMaster=" + isExistsInMaster +
                ", isRemovedFromMaster=" + isRemovedFromMaster +
                ", isNewlyAdded=" + isNewlyAdded +
                ", isSameAsMaster=" + isSameAsMaster +
                '}';
    }
}
