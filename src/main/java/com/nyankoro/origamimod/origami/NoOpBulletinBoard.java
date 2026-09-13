package com.nyankoro.origamimod.origami;

import origami.folding.util.IBulletinBoard;

public class NoOpBulletinBoard implements IBulletinBoard {

    @Override
    public void rewrite(int i, String s) {
    }

    @Override
    public void write(String s) {
    }

    @Override
    public void clear() {
    }
}