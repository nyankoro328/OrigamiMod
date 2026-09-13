package com.nyankoro.origamimod.origami;

import java.util.List;

public record OrigamiFoldResult(
        List<OrigamiFace> faces
) {
    public OrigamiFoldResult {
        faces = List.copyOf(faces);
    }
}