package model.algorithm;

/**
 * Enum que representa les dimensions de l'espai de punts (2D o 3D).
 * <p>
 * Aquest enum s'utilitza per indicar si un algoritme o visualització
 * s'executa en un espai bidimensional o tridimensional.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public enum Dimension {
    TWO_D("2D"),
    THREE_D("3D");

    private final String displayName;

    /**
     * Constructor per inicialitzar una instància de dimensió.
     * 
     * @param displayName el nom a mostrar en la interfície d'usuari
     */
    Dimension(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obté el nom a mostrar de la dimensió.
     * <p>
     * Aquest nom s'utilitza en la interfície d'usuari per representar
     * la dimensió de manera clara i comprensible.
     * </p>
     * 
     * @return el nom a mostrar de la dimensió
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obté la dimensió a partir del seu nom a mostrar.
     * <p>
     * Aquest mètode és útil per convertir un nom seleccionat per l'usuari
     * en la interfície gràfica al tipus d'enum corresponent.
     * </p>
     * 
     * @param displayName el nom a mostrar a cercar
     * @return la dimensió corresponent o null si no es troba
     */
    public static Dimension fromDisplayName(String displayName) {
        for (Dimension dim : values()) {
            if (dim.displayName.equals(displayName)) {
                return dim;
            }
        }
        return null;
    }
}
