# Point-Cloud Geometry Analyzer (2D/3D, MVC + JavaFX + Concurrency)

A compact, teaching-oriented JavaFX app to **generate, analyze, and visualize distances in 2D and 3D point clouds**. It compares multiple algorithms for **Closest Pair**, **Convex Hull (QuickHull 2D/3D)**, and **Diameter**, under a clean **Model–View–Controller** architecture with a **Factory Method** for pluggable algorithms and a reactive notification bus.&#x20;

> Built for Advanced Algorithms coursework. The accompanying (Catalan) report details design choices, algorithmic derivations, and complexity analyses.&#x20;

---

## Features

* **Interactive JavaFX GUI (2D & 3D)**
  Generate point clouds from multiple statistical distributions (Uniform, Gaussian, Exponential, Pareto, …), visualize them, and run algorithms with live feedback. 3D view supports camera orbit/zoom with smooth GPU-accelerated transforms.&#x20;
* **Algorithms you can swap in/out**

  * **Closest Pair**: brute force; **Divide-and-Conquer**; **KD-Tree** variant.&#x20;
  * **Convex Hull**: **QuickHull 2D** (parallelized) and **QuickHull3D** (adapted from Lloyd 2004, with adapters for project data types).&#x20;
  * **Diameter**: via hull + **rotating calipers** in 2D; vertex-pair search on the 3D hull.&#x20;
* **Concurrency done right**
  Fork/Join parallelism for heavy compute, JavaFX **Tasks** for off-UI work, and a **NotificationService** (observer pattern) to keep the UI reactive without tight coupling. Includes empirically tuned **sequential fallbacks/thresholds** for tiny subproblems.&#x20;
* **Extensible architecture**
  `AlgorithmFactory` wires `AlgorithmType × Dimension` → concrete implementation; results are normalized via `AlgorithmResult` for easy comparisons and plotting.&#x20;

---

## Architecture (MVC + events)

```
View (JavaFX)
  └─ MainView
        ↑ receives UI-safe updates via NotificationService
        ↓ user actions (run, params, focus/zoom)
Controller
  └─ MainController
        ↑ subscribes to notifications
        ↓ spins background Tasks, coordinates algorithms
Model (Algorithms & Data)
  ├─ PointGenerator (Uniform/Gaussian/Exponential/Pareto...)
  ├─ ClosestPair (Brute, D&V, KD-Tree)
  ├─ QuickHull2D / QuickHull3D (Lloyd-adapted via adapters)
  └─ Diameter (2D calipers, 3D vertex pairs)
Factory & Contracts
  ├─ AlgorithmFactory
  ├─ PointCloudAlgorithm<T>
  └─ AlgorithmResult
Infrastructure
  ├─ NotificationService / NotificationServiceImpl
  └─ UINotificationHandler
```

The **Factory Method** selects the right algorithm at runtime; the **observer-style** notification layer decouples long-running computation from the UI thread (updates are marshaled with `Platform.runLater`).&#x20;

---

## Algorithms & complexity (at a glance)

* **Closest Pair**
  * Brute force: **O(n²)** (2D/3D).
  * Divide-and-Conquer: **O(n log n)** in 2D; near **O(n log n)** in 3D with slab/neighbor limits and careful data ordering.
  * KD-Tree variant: expected **O(n log n)** after **O(n log n)** build.&#x20;
* **Convex Hull (QuickHull)**
  * Expected **O(n log n)**; **O(n²)** in degenerate worst cases (2D/3D). 3D uses an adapted QuickHull3D with robust numerics.&#x20;
* **Diameter**
  * 2D: **Hull + rotating calipers** → **O(n log n)** worst-case (dominated by hull).
  * 3D: **Hull + vertex-pair scan** → **O(n log n + h²)**, degrading to **O(n²)** if many points lie on the hull.&#x20;

---

## Concurrency model

* **Fork/Join work-stealing** for Divide-and-Conquer and bounded brute-force ranges; dynamic partitions balance uneven subproblems naturally.
* **Thresholds** switch to sequential code on tiny inputs to avoid task overhead.
* **JavaFX Tasks** keep the UI fluid; updates are funneled through `NotificationService` and applied on the JavaFX Application Thread.
* **3D view** uses GPU-accelerated transforms; long computations never block rendering.&#x20;

---

## Using the app

1. **Pick dimension**: 2D or 3D (the UI adjusts automatically to the chosen space).
2. **Generate points**: choose distribution and number of points; generation enforces bounds and uniqueness to keep samples valid.&#x20;
3. **Choose algorithm**: Closest Pair / Convex Hull / Diameter (with the desired variant).
4. **Run**: computations happen off the UI thread; watch progress and results; focus/zoom the output (2D pan/zoom; 3D orbit/zoom).&#x20;

---

## Key components (what they do)

| Component                                               | Role                                                                                                          |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `PointGenerator`                                        | Builds 2D/3D clouds from Uniform, Gaussian, Exponential, Pareto, etc., with bounds/dup-checks.                |
| `AlgorithmFactory`                                      | Factory Method: `(AlgorithmType, Dimension) → PointCloudAlgorithm<T>`.                                        |
| `PointCloudAlgorithm<T>`                                | Common contract (execute, metadata) for all algorithms.                                                       |
| `AlgorithmResult`                                       | Normalized result envelope for consistent display/compare.                                                    |
| `QuickHull2D`                                           | Native implementation; parallel branches via Fork/Join.                                                       |
| `QuickHull3D` (+ `Point3DAdapter`, `RobustQuickHull3D`) | Integration of Lloyd’s robust 3D hull with project types and triangle-facet extraction for rendering.         |
| `MainController`, `MainView`                            | Orchestrate runs on background threads; marshal UI updates with `NotificationService` & `Platform.runLater`.  |
| `NotificationService`, `UINotificationHandler`          | Reactive event bus for start/progress/completion/error updates.                                               |

---


## Further reading

* **Project report** (Catalan): *Distàncies entre punts dins núvols de punts. Desenvolupament, anàlisi i comparativa d’algorismes… (MVC)* — methods, concurrency design, and complexity proofs.&#x20;

---

## License

* The report text is released under **CC BY 4.0**. Consider adding a top-level `LICENSE` for the code (e.g., **MIT**) and referencing the report’s CC license for documentation.&#x20;

---

## Credits

* **Dylan Canning Garcia** and collaborators (see report). Thanks to course staff for guidance and review.&#x20;

---

### Citation (if you use this in teaching/research)

> Canning Garcia, D., et al. *Point-Cloud Geometry Analyzer (2D/3D, MVC + JavaFX + Concurrency).* Project code and report, 2025.&#x20;

