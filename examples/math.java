///usr/bin/env jbang "$0" "$@" ; exit $?

// https://github.com/mtommila/apfloat
// https://www.apfloat.org/apfloat_java/
// https://mvnrepository.com/artifact/org.apfloat/apfloat

//DEPS org.apfloat:apfloat:1.14.0

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

public class math
{
    public static void main(String[] args)
    {
        Apfloat x = new Apfloat(2, 1000);   // Value 2, precision 1000 digits

        Apfloat y = ApfloatMath.sqrt(x);    // Square root of 2, to 1000 digits

        System.out.println(y);
    }
}