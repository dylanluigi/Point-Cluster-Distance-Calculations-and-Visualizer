package model.algorithm;

/**
 * Enum que representa els tipus d'algoritmes disponibles a l'aplicació.
 * <p>
 * Aquest enum defineix els diferents algoritmes implementats juntament amb 
 * els seus noms de visualització i si utilitzen execució concurrent.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public enum AlgorithmType {
    CLOSEST_PAIR_NAIVE("Parell Més Proper (O(n²))", false),
    CLOSEST_PAIR_EFFICIENT("Parell Més Proper (O(n log n))", true),
    CLOSEST_PAIR_KDTREE("Parell Més Proper (KD-Tree)", false),
    CLOSEST_PAIR_ADAPTIVE("Parell Més Proper (Adaptatiu)", false),
    DIAMETER_NAIVE("Diàmetre (Naïf)", false),
    DIAMETER_CONCURRENT("Diàmetre (Concurrent)", true),
    DIAMETER_QUICKHULL("Diàmetre (QuickHall)", false);

    private final String displayName;
    private final boolean concurrent;

    /**
     * Constructor per inicialitzar una instància de tipus d'algoritme.
     * 
     * @param displayName el nom a mostrar en la interfície d'usuari
     * @param concurrent indica si l'algoritme utilitza execució concurrent
     */
    AlgorithmType(String displayName, boolean concurrent) {
        this.displayName = displayName;
        this.concurrent = concurrent;
    }

    /**
     * Obté el nom a mostrar de l'algoritme.
     * <p>
     * Aquest nom s'utilitza en la interfície d'usuari per representar
     * l'algoritme de manera amigable i comprensible.
     * </p>
     * 
     * @return el nom a mostrar de l'algoritme
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indica si l'algoritme utilitza execució concurrent.
     * <p>
     * Els algoritmes concurrents utilitzen múltiples fils d'execució i
     * són típicament més ràpids en màquines amb múltiples nuclis.
     * </p>
     * 
     * @return cert si l'algoritme utilitza execució concurrent, fals altrament
     */
    public boolean isConcurrent() {
        return concurrent;
    }

    /**
     * Obté el tipus d'algoritme a partir del seu nom a mostrar.
     * <p>
     * Aquest mètode és útil per convertir un nom seleccionat per l'usuari
     * en la interfície gràfica al tipus d'enum corresponent.
     * </p>
     * 
     * @param displayName el nom a mostrar a cercar
     * @return el tipus d'algoritme corresponent o null si no es troba
     */
    public static AlgorithmType fromDisplayName(String displayName) {
        for (AlgorithmType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }
}
