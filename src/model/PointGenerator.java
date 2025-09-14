package model;

import java.util.*;

/**
 * Aquesta classe genera punts aleatoris (2D i 3D) segons diverses distribucions.
 * Les distribucions disponibles són:
 * <ul>
 *   <li>UNIFORM: Distribució uniforme dins d'un rectangle (2D) o cub (3D).</li>
 *   <li>GAUSSIAN: Distribució normal (gaussiana) amb mitjana al centre del rang i desviació estàndard ajustada.</li>
 *   <li>EXPONENTIAL: Distribució exponencial per generar valors amb cua pesant.</li>
 *   <li>LOG_NORMAL: Distribució lognormal on l'exponencial d'una variable normal dóna el valor.</li>
 *   <li>RAYLEIGH: Distribució Rayleigh utilitzada per models de soroll i radar.</li>
 *   <li>PARETO: Distribució Pareto amb paràmetre d'escala i alfa = 2.</li>
 *   <li>CAUCHY: Distribució Cauchy amb cua molt pesada.</li>
 * </ul>
 */
public class PointGenerator {

    /**
     * Enumera les possibles distribucions estadístiques.
     */
    public enum Distribution {
        UNIFORM,
        GAUSSIAN,
        EXPONENTIAL,
        LOG_NORMAL,
        RAYLEIGH,
        PARETO,
        CAUCHY,
        WEIBULL,
        BETA,
        GAMMA,
        TRIANGULAR
    }

    // Instància de Random per a la generació de nombres aleatoris.
    private static final Random rand = new Random();

    /**
     * Genera una llista de punts 2D segons la distribució especificada.
     *
     * @param n      El nombre de punts a generar.
     * @param dist   La distribució a utilitzar.
     * @param width  L'amplada (valor màxim per x).
     * @param height L'alçada (valor màxim per y).
     * @return Una llista de Point2D generats.
     */
    public static List<Point2D> generate2DPoints(int n, Distribution dist, int width, int height) {
        List<Point2D> points = new ArrayList<>(n);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            double x = 0.0, y = 0.0;
            switch (dist) {
                case UNIFORM:
                    x = rand.nextDouble() * width;
                    y = rand.nextDouble() * height;
                    break;
                case GAUSSIAN:
                    x = rand.nextGaussian() * (width / 8.0) + width / 2.0;
                    y = rand.nextGaussian() * (height / 8.0) + height / 2.0;
                    break;
                case EXPONENTIAL:
                    double expX = -Math.log(1 - rand.nextDouble());
                    double expY = -Math.log(1 - rand.nextDouble());
                    x = expX * (width / 2.0);
                    y = expY * (height / 2.0);
                    break;
                case LOG_NORMAL:
                    x = Math.exp(rand.nextGaussian() * 0.5 + Math.log(width / 2.0));
                    y = Math.exp(rand.nextGaussian() * 0.5 + Math.log(height / 2.0));
                    break;
                case RAYLEIGH:
                    x = Math.sqrt(-2 * Math.log(1 - rand.nextDouble())) * (width / 2.0 / Math.sqrt(Math.PI / 2));
                    y = Math.sqrt(-2 * Math.log(1 - rand.nextDouble())) * (height / 2.0 / Math.sqrt(Math.PI / 2));
                    break;
                case PARETO:
                    x = Math.min(1 / Math.pow(1 - rand.nextDouble(), 1.0 / 2.0) * (width / 2.0), width - 0.001);
                    y = Math.min(1 / Math.pow(1 - rand.nextDouble(), 1.0 / 2.0) * (height / 2.0), height - 0.001);
                    break;
                case CAUCHY:
                    x = width / 2.0 + (width / 10.0) * Math.tan(Math.PI * (rand.nextDouble() - 0.5));
                    y = height / 2.0 + (height / 10.0) * Math.tan(Math.PI * (rand.nextDouble() - 0.5));
                    break;
                case WEIBULL:
                    double k = 1.5, lambda = width / 2.0;
                    x = lambda * Math.pow(-Math.log(1 - rand.nextDouble()), 1 / k);
                    lambda = height / 2.0;
                    y = lambda * Math.pow(-Math.log(1 - rand.nextDouble()), 1 / k);
                    break;
                case BETA:
                    double alpha = 2.0, beta = 5.0;
                    x = width * betaRand(alpha, beta);
                    y = height * betaRand(alpha, beta);
                    break;
                case GAMMA:
                    x = gammaRand(2.0, width / 4.0);
                    y = gammaRand(2.0, height / 4.0);
                    break;
                case TRIANGULAR:
                    x = triangularRand(0, width, width / 2);
                    y = triangularRand(0, height, height / 2);
                    break;
            }
            x = Math.max(0, Math.min(x, width - 0.001));
            y = Math.max(0, Math.min(y, height - 0.001));
            // Round to 4 decimal places for deduplication key to avoid floating point issues
            String key = String.format("%.4f,%.4f", x, y);
            if (!seen.contains(key)) {
                points.add(new Point2D(x, y));
                seen.add(key);
            }
        }
        return points;
    }

    /**
     * Genera una llista de punts 3D segons la distribució especificada.
     *
     * @param n     El nombre de punts a generar.
     * @param dist  La distribució a utilitzar.
     * @param maxX  Valor màxim per la coordenada x.
     * @param maxY  Valor màxim per la coordenada y.
     * @param maxZ  Valor màxim per la coordenada z.
     * @return Una llista de Point3D generats.
     */
    /**
     * Genera una llista de punts 3D segons la distribució especificada.
     *
     * @param n     El nombre de punts a generar.
     * @param dist  La distribució a utilitzar.
     * @param maxX  Valor màxim per la coordenada x.
     * @param maxY  Valor màxim per la coordenada y.
     * @param maxZ  Valor màxim per la coordenada z.
     * @return Una llista de Point3D generats.
     */
    public static List<Point3D> generate3DPoints(int n, Distribution dist, int maxX, int maxY, int maxZ) {
        List<Point3D> points = new ArrayList<>(n);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            double x = 0.0, y = 0.0, z = 0.0;
            switch (dist) {
                case UNIFORM:
                    x = rand.nextDouble() * maxX;
                    y = rand.nextDouble() * maxY;
                    z = rand.nextDouble() * maxZ;
                    break;
                case GAUSSIAN:
                    x = rand.nextGaussian() * (maxX / 8.0) + maxX / 2.0;
                    y = rand.nextGaussian() * (maxY / 8.0) + maxY / 2.0;
                    z = rand.nextGaussian() * (maxZ / 8.0) + maxZ / 2.0;
                    break;
                case EXPONENTIAL:
                    double expX = -Math.log(1 - rand.nextDouble());
                    double expY = -Math.log(1 - rand.nextDouble());
                    double expZ = -Math.log(1 - rand.nextDouble());
                    x = expX * (maxX / 2.0);
                    y = expY * (maxY / 2.0);
                    z = expZ * (maxZ / 2.0);
                    break;
                case LOG_NORMAL:
                    double muX = Math.log(maxX / 2.0);
                    double sigmaX = 0.5;
                    x = Math.exp(rand.nextGaussian() * sigmaX + muX);
                    double muY = Math.log(maxY / 2.0);
                    double sigmaY = 0.5;
                    y = Math.exp(rand.nextGaussian() * sigmaY + muY);
                    double muZ = Math.log(maxZ / 2.0);
                    double sigmaZ = 0.5;
                    z = Math.exp(rand.nextGaussian() * sigmaZ + muZ);
                    break;
                case RAYLEIGH:
                    double sigmaRayleighX = (maxX / 2.0) / Math.sqrt(Math.PI / 2);
                    double sigmaRayleighY = (maxY / 2.0) / Math.sqrt(Math.PI / 2);
                    double sigmaRayleighZ = (maxZ / 2.0) / Math.sqrt(Math.PI / 2);
                    x = sigmaRayleighX * Math.sqrt(-2 * Math.log(1 - rand.nextDouble()));
                    y = sigmaRayleighY * Math.sqrt(-2 * Math.log(1 - rand.nextDouble()));
                    z = sigmaRayleighZ * Math.sqrt(-2 * Math.log(1 - rand.nextDouble()));
                    break;
                case PARETO:
                    double alfa = 2.0;
                    double xPareto = 1 / Math.pow(1 - rand.nextDouble(), 1.0 / alfa);
                    double yPareto = 1 / Math.pow(1 - rand.nextDouble(), 1.0 / alfa);
                    double zPareto = 1 / Math.pow(1 - rand.nextDouble(), 1.0 / alfa);
                    x = Math.min(xPareto * (maxX / 2.0), maxX - 0.001);
                    y = Math.min(yPareto * (maxY / 2.0), maxY - 0.001);
                    z = Math.min(zPareto * (maxZ / 2.0), maxZ - 0.001);
                    break;
                case CAUCHY:
                    double locationX = maxX / 2.0;
                    double scaleX = maxX / 10.0;
                    x = locationX + scaleX * Math.tan(Math.PI * (rand.nextDouble() - 0.5));
                    double locationY = maxY / 2.0;
                    double scaleY = maxY / 10.0;
                    y = locationY + scaleY * Math.tan(Math.PI * (rand.nextDouble() - 0.5));
                    double locationZ = maxZ / 2.0;
                    double scaleZ = maxZ / 10.0;
                    z = locationZ + scaleZ * Math.tan(Math.PI * (rand.nextDouble() - 0.5));
                    break;
                case WEIBULL:
                    double k = 1.5, lambda = maxX / 2.0;
                    x = lambda * Math.pow(-Math.log(1 - rand.nextDouble()), 1 / k);
                    lambda = maxY / 2.0;
                    y = lambda * Math.pow(-Math.log(1 - rand.nextDouble()), 1 / k);
                    lambda = maxZ / 2.0;
                    z = lambda * Math.pow(-Math.log(1 - rand.nextDouble()), 1 / k);
                    break;
                case BETA:
                    double alpha = 2.0, beta = 5.0;
                    x = maxX * betaRand(alpha, beta);
                    y = maxY * betaRand(alpha, beta);
                    z = maxZ * betaRand(alpha, beta);
                    break;
                case GAMMA:
                    x = gammaRand(2.0, maxX / 4.0);
                    y = gammaRand(2.0, maxY / 4.0);
                    z = gammaRand(2.0, maxZ / 4.0);
                    break;
                case TRIANGULAR:
                    x = triangularRand(0, maxX, maxX / 2);
                    y = triangularRand(0, maxY, maxY / 2);
                    z = triangularRand(0, maxZ, maxZ / 2);
                    break;
            }
            x = Math.max(0, Math.min(x, maxX - 0.001));
            y = Math.max(0, Math.min(y, maxY - 0.001));
            z = Math.max(0, Math.min(z, maxZ - 0.001));
            // Round to 4 decimal places for deduplication key to avoid floating point issues
            String key = String.format("%.4f,%.4f,%.4f", x, y, z);
            if (!seen.contains(key)) {
                points.add(new Point3D(x, y, z));
                seen.add(key);
            }
        }
        return points;
    }


    private static double betaRand(double alpha, double beta) {
        double x = gammaRand(alpha, 1.0);
        double y = gammaRand(beta, 1.0);
        return x / (x + y);
    }

    private static double gammaRand(double shape, double scale) {
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9 * d);
        while (true) {
            double x = rand.nextGaussian();
            double v = 1 + c * x;
            if (v <= 0) continue;
            v = v * v * v;
            double u = rand.nextDouble();
            if (u < 1 - 0.0331 * x * x * x * x) return scale * d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) return scale * d * v;
        }
    }

    private static double triangularRand(double min, double max, double mode) {
        double u = rand.nextDouble();
        double c = (mode - min) / (max - min);
        if (u < c) return min + Math.sqrt(u * (max - min) * (mode - min));
        else return max - Math.sqrt((1 - u) * (max - min) * (max - mode));
    }
}
