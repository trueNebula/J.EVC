# J.EVC
JPEG based H.261 inspired video codec created as part of my bachelor thesis.

Supports encoding a set of frames both into AVI MJPEG and into a proprietary format designed to emulate the technologies used by H.261.

A semi functional decoder implementation is also included.

## Installation
The codec requires Java 20. 

```bash
$ git clone
$ cd J.EVC
$ mvn clean compile assembly:single
$ java jevc.JVidEncoder 
```

## Usage
The codec is designed to be used from the command line. The following CLI flags are available:

- -i : Input file path
- -o : Output file path
- -f : Frame rate
- -p: Use parallelization
  - f: Per-Frame
  - g: Per-GOP
  - o: Frame Operation
  - c: Combination
- -m: Compress to MJPEG
- -b: Enable benchmarking
- -e export: Export benchmark
- -d: Create debug frames
- -q: Quiet mode
- -h: Help

Example:

```bash
jevc -i inputFolder -o outputFolder/out.jvd -f 30 -p c -b -e ./benchmark.txt -q
```

The decoder requires ffmpeg to be installed. Due to it's unfinished nature, no documentation will be provided. Have fun :)

## License
[GPL V3](https://choosealicense.com/licenses/gpl-3.0/)
