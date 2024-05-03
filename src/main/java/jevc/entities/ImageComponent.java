package jevc.entities;

public class ImageComponent {
    /* each component is described by 3 bytes of data:
     *	    .id : 1=Y, 2=Cb, 3=Cr, 4=I, 5=Q
     *	    .sampling factors : bits 0-3 vertical, 4-7 horizontal
     *	    .quantization table number
     *      .huffman table number
     */
    public int id;
    public int verticalSampling, horizontalSampling;
    public int quantizationTblIxd;
    public int AChuffmanTblIdx;
    public int DChuffmanTblIdx;

    public ImageComponent() {
        id = -1;
        verticalSampling = horizontalSampling = -1;
        quantizationTblIxd = -1;
        AChuffmanTblIdx = -1;
        DChuffmanTblIdx = -1;
    }
}
