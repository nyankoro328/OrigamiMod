package com.nyankoro.origamimod.origami;

import origami.crease_pattern.FoldingException;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.PointSet;
import origami.folding.FoldedFigure;
import origami.folding.util.SortingBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class OrieditaAdapter {

    private OrieditaAdapter() {
    }

    public static OrigamiFoldResult fold(InputStream cpInput)
            throws IOException, InterruptedException, FoldingException {

        // .cp → Orieditaの線分集合
        LineSegmentSet lineSegmentSet =
                CpLoader.load(cpInput);

        // 折り畳みエンジン
        FoldedFigure foldedFigure =
                new FoldedFigure(new NoOpBulletinBoard());

        foldedFigure.estimationOrder =
                FoldedFigure.EstimationOrder.ORDER_5;

        // Orieditaのテストと同じく基準Faceは1
        foldedFigure.folding_estimated(
                lineSegmentSet,
                1
        );

        // 元Faceを維持した折り畳み後PointSet
        PointSet folded =
                foldedFigure
                        .wireFrameWorker_foldedNotSubdivided
                        .get();

        // 下→上の描画順
        SortingBox<Integer> renderOrder =
                foldedFigure
                        .foldedFigure_worker
                        .rating2();

        List<OrigamiFace> faces = new ArrayList<>();

        for (int orderIndex = 1;
             orderIndex <= renderOrder.getTotal();
             orderIndex++) {

            int faceId =
                    renderOrder.getValue(orderIndex);

            // 奇数なら表、偶数なら裏
            int facePosition =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(faceId);

            boolean frontSideUp =
                    (facePosition % 2) == 1;

            int vertexCount =
                    folded.getPointsCount(faceId);

            List<OrigamiVertex> vertices =
                    new ArrayList<>();

            for (int vertexIndex = 1;
                 vertexIndex <= vertexCount;
                 vertexIndex++) {

                int pointId =
                        folded.getPointId(
                                faceId,
                                vertexIndex
                        );

                vertices.add(
                        new OrigamiVertex(
                                folded.getPointX(pointId),
                                folded.getPointY(pointId)
                        )
                );
            }

            faces.add(
                    new OrigamiFace(
                            faceId,
                            vertices,
                            frontSideUp,
                            orderIndex
                    )
            );
        }

        return new OrigamiFoldResult(faces);
    }
}