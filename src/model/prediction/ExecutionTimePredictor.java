package model.prediction;

import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe per predir el temps d'execució d'algoritmes basant-se en models de complexitat.
 * <p>
 * Aquesta classe utilitza regressió sobre resultats anteriors per calibrar models predictius
 * per a diferents tipus d'algoritmes. Suporta models de complexitat O(n), O(n log n) i O(n²).
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ExecutionTimePredictor {

    private final Map<AlgorithmType, ModelParameters> modelMap = new HashMap<>();
    
    /**
     * Classe interna per emmagatzemar els paràmetres dels models de regressió.
     */
    private static class ModelParameters {
        double a, b, c;  // Coeficients del model
        ComplexityModel modelType;  // Tipus de model (lineal, nlogn, quadratic)
        
        ModelParameters(double a, double b, double c, ComplexityModel modelType) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.modelType = modelType;
        }
    }
    
    /**
     * Enumeració dels models de complexitat suportats.
     */
    public enum ComplexityModel {
        LINEAR,      // O(n)
        NLOGN,       // O(n log n)
        QUADRATIC    // O(n²)
    }
    
    /**
     * Constructor per inicialitzar el predictor amb els models per defecte.
     * <p>
     * Assigna el model de complexitat adequat a cada tipus d'algoritme
     * basant-se en la seva complexitat teòrica.
     * </p>
     */
    public ExecutionTimePredictor() {
        // Inicialitzem models per defecte per a cada tipus d'algoritme

        modelMap.put(AlgorithmType.CLOSEST_PAIR_NAIVE, 
                new ModelParameters(1.5e-6, 1.0e-5, 0.5, ComplexityModel.QUADRATIC));
        
        modelMap.put(AlgorithmType.CLOSEST_PAIR_EFFICIENT, 
                new ModelParameters(8.0e-7, 5.0e-6, 1.0, ComplexityModel.NLOGN));
        
        modelMap.put(AlgorithmType.CLOSEST_PAIR_KDTREE, 
                new ModelParameters(9.0e-7, 7.0e-6, 1.5, ComplexityModel.NLOGN));
        
        // Algoritmes de diàmetre
        modelMap.put(AlgorithmType.DIAMETER_NAIVE, 
                new ModelParameters(1.2e-6, 8.0e-6, 0.3, ComplexityModel.QUADRATIC));
        
        modelMap.put(AlgorithmType.DIAMETER_CONCURRENT, 
                new ModelParameters(6.0e-7, 3.0e-6, 2.0, ComplexityModel.QUADRATIC));
        
        modelMap.put(AlgorithmType.DIAMETER_QUICKHULL, 
                new ModelParameters(1.0e-6, 6.0e-6, 1.0, ComplexityModel.NLOGN));
    }
    
    /**
     * Prediu el temps d'execució per a un algoritme determinat amb un nombre de punts específic.
     * <p>
     * Utilitza el model de regressió calibrat per al tipus d'algoritme per estimar el temps d'execució.
     * </p>
     * 
     * @param type el tipus d'algoritme
     * @param numPoints el nombre de punts a processar
     * @return el temps predit en mil·lisegons, o -1 si el tipus d'algoritme no és suportat
     */
    public double predictExecutionTime(AlgorithmType type, int numPoints) {
        ModelParameters params = modelMap.get(type);
        if (params == null) {
            return -1.0;
        }
        
        switch (params.modelType) {
            case LINEAR:
                return params.a * numPoints + params.b + params.c;
                
            case NLOGN:
                return params.a * numPoints * Math.log(numPoints) + params.b * numPoints + params.c;
                
            case QUADRATIC:
                return params.a * numPoints * numPoints + params.b * numPoints + params.c;
                
            default:
                return -1.0;
        }
    }
    
    /**
     * Calibra el model de predicció per a un tipus d'algoritme específic utilitzant dades d'execucions anteriors.
     * <p>
     * Aquest mètode ajusta els coeficients del model mitjançant regressió lineal per minimitzar l'error quadràtic.
     * </p>
     * 
     * @param type el tipus d'algoritme a calibrar
     * @param sampleSizes llista dels tamanys de mostra utilitzats
     * @param executionTimes llista dels temps d'execució corresponents en mil·lisegons
     * @return cert si la calibració va ser exitosa
     */
    public boolean calibrateModel(AlgorithmType type, List<Integer> sampleSizes, List<Long> executionTimes) {
        if (sampleSizes.size() != executionTimes.size() || sampleSizes.isEmpty()) {
            return false;
        }
        
        ModelParameters params = modelMap.get(type);
        if (params == null) {
            // Si no tenim un model per aquest tipus, creem un per defecte
            params = new ModelParameters(0, 0, 0, determineComplexityModel(type));
            modelMap.put(type, params);
        }
        
        // Apliquem regressió lineal múltiple segons el model de complexitat
        switch (params.modelType) {
            case LINEAR:
                calibrateLinearModel(params, sampleSizes, executionTimes);
                break;
            case NLOGN:
                calibrateNLogNModel(params, sampleSizes, executionTimes);
                break;
            case QUADRATIC:
                calibrateQuadraticModel(params, sampleSizes, executionTimes);
                break;
        }
        
        return true;
    }
    
    /**
     * Calibra un model lineal (O(n)) utilitzant regressió lineal.
     * 
     * @param params paràmetres del model a ajustar
     * @param sampleSizes tamanys de mostra utilitzats
     * @param executionTimes temps d'execució corresponents
     */
    private void calibrateLinearModel(ModelParameters params, List<Integer> sampleSizes, List<Long> executionTimes) {
        // Implementem regressió lineal simple (y = a*x + c)
        int n = sampleSizes.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = sampleSizes.get(i);
            double y = executionTimes.get(i);
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) > 1e-10) {
            params.a = (n * sumXY - sumX * sumY) / denominator;
            params.c = (sumY - params.a * sumX) / n;
        }
        
        // En el model lineal, b no s'utilitza
        params.b = 0;
    }
    
    /**
     * Calibra un model O(n log n) utilitzant regressió lineal múltiple.
     * 
     * @param params paràmetres del model a ajustar
     * @param sampleSizes tamanys de mostra utilitzats
     * @param executionTimes temps d'execució corresponents
     */
    private void calibrateNLogNModel(ModelParameters params, List<Integer> sampleSizes, List<Long> executionTimes) {
        int n = sampleSizes.size();

        double[][] X = new double[n][3];  // [x*log(x), x, 1]
        double[] Y = new double[n];       // [y]
        
        for (int i = 0; i < n; i++) {
            double x = sampleSizes.get(i);
            double y = executionTimes.get(i);
            double xLogX = x * Math.log(x);
            
            X[i][0] = xLogX;
            X[i][1] = x;
            X[i][2] = 1;
            Y[i] = y;
        }

        double[] coefficients = solveLinearSystem(X, Y);
        if (coefficients != null) {
            params.a = coefficients[0];
            params.b = coefficients[1];
            params.c = coefficients[2];
        }
    }
    
    /**
     * Calibra un model quadràtic (O(n²)) utilitzant regressió lineal múltiple.
     * 
     * @param params paràmetres del model a ajustar
     * @param sampleSizes tamanys de mostra utilitzats
     * @param executionTimes temps d'execució corresponents
     */
    private void calibrateQuadraticModel(ModelParameters params, List<Integer> sampleSizes, List<Long> executionTimes) {
        int n = sampleSizes.size();

        double[][] X = new double[n][3];  // [x², x, 1]
        double[] Y = new double[n];       // [y]
        
        for (int i = 0; i < n; i++) {
            double x = sampleSizes.get(i);
            double y = executionTimes.get(i);
            double x2 = x * x;
            
            X[i][0] = x2;
            X[i][1] = x;
            X[i][2] = 1;
            Y[i] = y;
        }
        
        // Resolvem el sistema d'equacions normalment
        double[] coefficients = solveLinearSystem(X, Y);
        if (coefficients != null) {
            params.a = coefficients[0];
            params.b = coefficients[1];
            params.c = coefficients[2];
        }
    }
    
    /**
     * Resol un sistema d'equacions lineals utilitzant el mètode de Gauss-Jordan.
     * 
     * @param X matriu de coeficients
     * @param Y vector de termes independents
     * @return array amb els coeficients resolts, o null si no es pot resoldre
     */
    private double[] solveLinearSystem(double[][] X, double[] Y) {
        int n = X.length;

        if (n < 3) {
            return new double[] {0.0000001, 0.0000005, 0.1};
        }
        
        int m = X[0].length;
        
        // Creem la matriu augmentada [X|Y]
        double[][] augmented = new double[n][m + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(X[i], 0, augmented[i], 0, m);
            augmented[i][m] = Y[i];
        }
        
        // Gauss-Jordan eliminació
        for (int i = 0; i < Math.min(m, n); i++) {
            // Troba el pivot
            int maxRow = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(augmented[j][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = j;
                }
            }
            
            // Intercanvia files si cal
            if (maxRow != i) {
                double[] temp = augmented[i];
                augmented[i] = augmented[maxRow];
                augmented[maxRow] = temp;
            }
            
            // Comprova si és singular
            if (Math.abs(augmented[i][i]) < 1e-10) {
                continue;  // Matriu singular, però intentem continuar
            }
            
            // Normalitza la fila del pivot
            double pivotVal = augmented[i][i];
            for (int j = i; j <= m; j++) {
                augmented[i][j] /= pivotVal;
            }
            
            // Elimina aquesta variable de les altres files
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    double factor = augmented[j][i];
                    for (int k = i; k <= m; k++) {
                        augmented[j][k] -= factor * augmented[i][k];
                    }
                }
            }
        }
        
        // Extreu la solució
        double[] solution = new double[m];
        for (int i = 0; i < Math.min(m, n); i++) {
            solution[i] = augmented[i][m];
        }
        
        // If we couldn't solve for all coefficients, set defaults for missing ones
        for (int i = Math.min(m, n); i < m; i++) {
            solution[i] = 0.1; // Default value
        }
        
        return solution;
    }
    
    /**
     * Determina el model de complexitat adequat per a un tipus d'algoritme.
     * 
     * @param type el tipus d'algoritme
     * @return el model de complexitat teòric per a aquest algoritme
     */
    private ComplexityModel determineComplexityModel(AlgorithmType type) {
        switch (type) {
            case CLOSEST_PAIR_NAIVE:
            case DIAMETER_NAIVE:
            case DIAMETER_CONCURRENT:
                return ComplexityModel.QUADRATIC;
                
            case CLOSEST_PAIR_EFFICIENT:
            case CLOSEST_PAIR_KDTREE:
            case DIAMETER_QUICKHULL:
                return ComplexityModel.NLOGN;
                
            default:
                return ComplexityModel.LINEAR;  // Per defecte, assumim lineal
        }
    }
    
    /**
     * Calibra el model utilitzant resultats d'execucions anteriors.
     * 
     * @param results llista de resultats d'algoritmes
     * @return cert si la calibració va ser exitosa
     */
    // NO UTILITZAT
    public boolean calibrateModel(List<AlgorithmResult<?>> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }

        Map<AlgorithmType, List<AlgorithmResult<?>>> resultsByType = new HashMap<>();
        for (AlgorithmResult<?> result : results) {
            resultsByType.computeIfAbsent(result.getAlgorithmType(), k -> new java.util.ArrayList<>())
                         .add(result);
        }

        boolean success = true;
        for (Map.Entry<AlgorithmType, List<AlgorithmResult<?>>> entry : resultsByType.entrySet()) {
            AlgorithmType type = entry.getKey();
            List<AlgorithmResult<?>> typeResults = entry.getValue();
            

            if (typeResults.size() < 3) {
                System.out.println("Not enough data points for " + type + " (minimum 3 required, found " + 
                    typeResults.size() + ")");
                continue;
            }

            List<Integer> sampleSizes = new java.util.ArrayList<>();
            List<Long> executionTimes = new java.util.ArrayList<>();

            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;
            
            for (AlgorithmResult<?> result : typeResults) {
                long time = result.getExecutionTimeMs();
                if (time < minTime) minTime = time;
                if (time > maxTime) maxTime = time;
            }

            for (AlgorithmResult<?> result : typeResults) {
                long time = result.getExecutionTimeMs();
                int sampleSize = 100;
                if (maxTime > minTime) {
                    sampleSize = 100 + (int)(9900.0 * (time - minTime) / (maxTime - minTime));
                }
                sampleSizes.add(sampleSize);
                executionTimes.add(time);
            }

            if (!calibrateModel(type, sampleSizes, executionTimes)) {
                success = false;
            }
        }
        
        return success;
    }
    
    /**
     * Obté els paràmetres del model per a un algoritme específic.
     * 
     * @param type el tipus d'algoritme
     * @return un array amb els paràmetres [a, b, c] del model, o null si no existeix
     */
    public double[] getModelParameters(AlgorithmType type) {
        ModelParameters params = modelMap.get(type);
        if (params == null) {
            return null;
        }
        return new double[] {params.a, params.b, params.c};
    }
    
    /**
     * Obté el tipus de model de complexitat per a un algoritme específic.
     * 
     * @param type el tipus d'algoritme
     * @return el model de complexitat, o null si no existeix
     */
    public ComplexityModel getModelType(AlgorithmType type) {
        ModelParameters params = modelMap.get(type);
        return params != null ? params.modelType : null;
    }
    
    /**
     * Estableix els paràmetres del model per a un algoritme específic.
     * <p>
     * Aquest mètode permet configurar manualment els paràmetres del model.
     * </p>
     * 
     * @param type el tipus d'algoritme
     * @param a coeficient per al terme principal (n², n log n, o n)
     * @param b coeficient per al terme lineal
     * @param c terme constant
     * @param modelType tipus de model de complexitat
     */
    public void setModelParameters(AlgorithmType type, double a, double b, double c, ComplexityModel modelType) {
        modelMap.put(type, new ModelParameters(a, b, c, modelType));
    }
}