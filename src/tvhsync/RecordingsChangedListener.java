package tvhsync;

import java.util.List;

import tvhsync.model.Recording;

public interface RecordingsChangedListener {

	void recordingsChanged(List<Recording> recordings);
}
