package io.th0rgal.oraxen.utils.general;

import java.io.File;

public final class Constants {

    /*
     * 
     */

    public enum Folders {

        MAIN("main", "plugins//Oraxen"),

        TRANSLATION("translation", "%main%//language", MAIN),;

        /*
         * 
         */

        private final String var;
        private final String value;

        private final Folders[] needed;

        private Folders(String var, String value, Folders... needed) {
            this.var = '%' + var + '%';
            this.value = value;
            this.needed = needed;
        }

        /*
         * 
         */

        public String getVar() {
            return var;
        }

        public String getVarPath() {
            return value;
        }

        /*
         * 
         */

        public String getPath() {
            if (needed.length == 0)
                return value;
            String output = value;
            for (Folders need : needed)
                output = output.replace(need.getVar(), need.getPath());
            return output;
        }

        public File getFile() {
            return new File(getPath());
        }

    }

}
