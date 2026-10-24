package tvhsync.model;

import tvhsync.model.SizeFormatter;

public class DiskStatus {
    private long spaceTotalInBytes;
    private long spaceFreeInBytes;
    private int usage;

    public long getSpaceTotalInBytes() {
        return spaceTotalInBytes;
    }

    public void setSpaceTotalInBytes(long spaceTotalInBytes) {
        this.spaceTotalInBytes = spaceTotalInBytes;
    }

    public long getSpaceFreeInBytes() {
        return spaceFreeInBytes;
    }

    public void setSpaceFreeInBytes(long spaceFreeInBytes) {
        this.spaceFreeInBytes = spaceFreeInBytes;
    }

    public int getUsage() {
        return usage;
    }

    public void setUsage(int usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        return SizeFormatter.format(spaceFreeInBytes) + "  " + usage + "%";
    }

}
