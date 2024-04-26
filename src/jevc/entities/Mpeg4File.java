package jevc.entities;

public class Mpeg4File {
}

class Box {
    private final int size;
    private final String name;

    public Box(int size, String name) {
        this.size = size;
        this.name = name;
    }
}

class FiletypeBox extends Box {
    private final String majorBrand;
    private final int minorVersion;
    private final String[] compatibleBrands;

    public FiletypeBox(int size, String name, String majorBrand, int minorVersion, String[] compatibleBrands) {
        super(size, name);
        this.majorBrand = majorBrand;
        this.minorVersion = minorVersion;
        this.compatibleBrands = compatibleBrands;
    }
}

class MoovHeaderBox extends Box {
    private final int flags;
    private final byte version;
    private final int creationTime;
    private final int modificationTime;
    private final int timescale;
    private final int duration;
    private final int rate;
    private final int volume;

    public MoovHeaderBox(int size, String name, int flags, byte version, int creationTime, int modificationTime, int timescale, int duration, int rate, int volume) {
        super(size, name);
        this.flags = flags;
        this.version = version;
        this.creationTime = creationTime;
        this.modificationTime = modificationTime;
        this.timescale = timescale;
        this.duration = duration;
        this.rate = rate;
        this.volume = volume;
    }
}

class MoovBox extends Box {
    private final MoovHeaderBox MoovHeader;

    public MoovBox(int size, String name, MoovHeaderBox moovHeader) {
        super(size, name);
        MoovHeader = moovHeader;
    }
}
