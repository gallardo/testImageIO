DESCRIPTION

    This package contains a Java program that allows to reproduce an error that
    surfaces when reading pictures with an associated color profile. This error
    is reproducible only with a SUSE openjdk, and only when reading pictures
    with associated color profiles.

PACKAGE CONTENT

    TestImageIO.java
        Source code to reproduce the problem. The class has a usage help

    testImage_sRGB.jpg
        Picture with associated sRGB color profile

    testImage_noProfile.jpg
        Picture w/o associated color profile. For comparison. This is always
        loaded correctly


PROBLEM DESCRIPTION

    ImageIO API is not thread safe: The parallel reading of images with
    associated color profiles results in failure or incorrect interpretation of
    the corresponding color profiles. The images are incorrectly rendered
    afterwards.

    After the first failure reading an image, are all further readings of
    images using ImageIO also incorrect and the jvm must be restarted to read
    images correctly again.

    This failure has been consistently reproduced using the SUSE package
    java-1_8_0-openjdk:

    Version        : 1.8.0.222-27.35.2
    Arch           : x86_64
    Vendor         : SUSE LLC <https://www.suse.com/>

    Version        : 1.8.0.212-34.1
    Arch           : x86_64
    Vendor         : openSUSE

    Version        : 1.8.0.222-lp151.2.3.1
    Arch           : x86_64
    Vendor         : openSUSE


STEPS TO REPRODUCE

    # Compile the script

        $ javac TestImageIO.java
        Note: TestImageIO.java uses unchecked or unsafe operations.
        Note: Recompile with -Xlint:unchecked for details.

    # Run the script.
    # The script reads the file 'testImage_sRGB.jpg' from the current
    # directory. I could always reproduce the problem using 50 iterations:

        $ java TestImageIO 5 50 false; echo $?
        Starting
        Loop 0
        Callables submitted. Collecting results
        Embedded color profile is invalid; ignored
        Embedded color profile is invalid; ignored
        Embedded color profile is invalid; ignored
            (  0,  1) !!!bi.getRGB(0,0) = ff030103. Expected: ff010103
            (  0,  4) !!!bi.getRGB(0,0) = ff030103. Expected: ff010103
        .
        .
        .
        # output truncated
        .
        .
        .
            (  9,  0) !!!bi.getRGB(0,0) = ff023006. Expected: ff010103
            success (9): false
        1

    A non zero exit value signals that the problem has been reproduced.


