/*
 * Md. Momin Al Aziz momin.aziz.cse @ gmail.com	
 * http://www.mominalaziz.com
 */
package org.umanitoba.smc_genome;

import flexsc.Flag;
import java.math.BigInteger;
import java.util.Arrays;
import util.ConfigParser;
import util.GenRunnable;

/**
 *
 * @author shad942
 */
public class GCOperation {

    public static int getDecryptionWithGC(BigInteger input,String key) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ConfigParser config = new ConfigParser("E:\\Privacy Code Works\\SecureWebGenomic\\smc_genome\\Config");
        String[] args = new String[3];
        args[0] = "example.DecryptHE";
        args[1] = key;
        args[2] = input.toString();
        System.out.println("before "+args[2]);
        Class<?> clazz = Class.forName(args[0] + "$Generator");
        GenRunnable run = (GenRunnable) clazz.newInstance();
        run.setParameter(config, Arrays.copyOfRange(args, 1, args.length));//removing the classname
        run.run();
        if (Flag.CountTime) {
            Flag.sw.print();
        }
        return 0;
    }
}
