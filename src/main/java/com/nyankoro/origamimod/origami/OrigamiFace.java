package com.nyankoro.origamimod.origami;

import java.util.List;

public record OrigamiFace(
        int faceId,
        List<OrigamiVertex> vertices,
        boolean frontSideUp,
        int renderOrder
) {
    public OrigamiFace {
        vertices = List.copyOf(vertices);
    }
}