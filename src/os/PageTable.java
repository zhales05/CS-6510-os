package os;

public class PageTable {
    private final PageTableEntry[] entries;

    public PageTable(int numberOfPages) {
        entries = new PageTableEntry[numberOfPages];
        for (int i = 0; i < numberOfPages; i++) {
            entries[i] = new PageTableEntry(); // Create empty entries
        }
    }

    public void setEntry(int pageNumber, PageTableEntry entry) {
        entries[pageNumber] = entry;
    }

    public PageTableEntry getEntry(int pageNumber) {
        return entries[pageNumber];
    }

    public int getNumberOfPages() {
        return entries.length;
    }
}
