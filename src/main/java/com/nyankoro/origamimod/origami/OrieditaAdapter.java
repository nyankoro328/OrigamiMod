package com.nyankoro.origamimod.origami;

import origami.crease_pattern.FoldingException;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.PointSet;
import origami.folding.FoldedFigure;
import origami.folding.element.SubFace;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class OrieditaAdapter {

    private OrieditaAdapter() {
    }

    public static OrigamiFoldResult fold(InputStream cpInput)
            throws IOException, InterruptedException, FoldingException {

        LineSegmentSet lineSegmentSet =
                CpLoader.load(cpInput);

        FoldedFigure foldedFigure =
                new FoldedFigure(new NoOpBulletinBoard());

        foldedFigure.estimationOrder =
                FoldedFigure.EstimationOrder.ORDER_5;

        foldedFigure.folding_estimated(
                lineSegmentSet,
                1
        );

        /*
         * 重要：
         * 元Faceではなく、折り畳み後に細分割された
         * SubFaceのPointSetを使用する。
         */
        PointSet subdivided =
                foldedFigure
                        .wireFrameWorker_foldedSubdivided
                        .get();

        SubFace[] subFaces =
                foldedFigure
                        .foldedFigure_worker
                        .s0;

        List<OrigamiFace> visibleRegions =
                new ArrayList<>();

        int regionCount =
                subdivided.getNumFaces();

        for (int regionId = 1;
             regionId <= regionCount;
             regionId++) {

            SubFace subFace =
                    subFaces[regionId];

            // 紙が存在しない領域は描画しない
            if (subFace == null
                    || subFace.getFaceIdCount() == 0) {
                continue;
            }

            /*
             * このSubFaceでの上下関係を計算する。
             */
            subFace.set_FaceId2fromTop_counted_position(
                    foldedFigure
                            .foldedFigure_worker
                            .hierarchyList
            );

            /*
             * 通常の正面表示なので、
             * 上から1番目の元Faceを取得する。
             */
            int topFaceId =
                    subFace.fromTop_count_FaceId(1);

            int facePosition =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(topFaceId);

            boolean frontSideUp =
                    (facePosition % 2) == 1;

            int vertexCount =
                    subdivided.getPointsCount(regionId);

            if (vertexCount < 3) {
                continue;
            }

            List<OrigamiVertex> vertices =
                    new ArrayList<>();

            for (int vertexIndex = 1;
                 vertexIndex <= vertexCount;
                 vertexIndex++) {

                int pointId =
                        subdivided.getPointId(
                                regionId,
                                vertexIndex
                        );

                vertices.add(
                        new OrigamiVertex(
                                subdivided.getPointX(pointId),
                                subdivided.getPointY(pointId)
                        )
                );
            }

            /*
             * faceIdには、この領域で実際に見えている
             * 元FaceのIDを格納。
             *
             * renderOrderは今回使わないため0。
             */
            visibleRegions.add(
                    new OrigamiFace(
                            topFaceId,
                            vertices,
                            frontSideUp,
                            0
                    )
            );
        }

        return new OrigamiFoldResult(
                visibleRegions
        );
    }
}