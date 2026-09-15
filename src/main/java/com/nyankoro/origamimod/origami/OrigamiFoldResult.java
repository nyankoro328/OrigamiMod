package com.nyankoro.origamimod.origami;

import java.util.List;

public record OrigamiFoldResult(
        List<OrigamiFace> faces,
        List<OrigamiEdge> edges
) {

    public OrigamiFoldResult {
        faces = List.copyOf(faces);
        edges = List.copyOf(edges);
    }

    /*
     * 既存コードとの互換用。
     */
    public OrigamiFoldResult(
            List<OrigamiFace> faces
    ) {
        this(
                faces,
                List.of()
        );
    }
}