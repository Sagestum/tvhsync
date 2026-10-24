package tvhsync;

import tvhsync.model.DiskStatus;

public interface DiskStatusListener {
	void diskStatusChanged(DiskStatus status);
}
