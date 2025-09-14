#!/usr/bin/env python3
"""
Visualization script for comprehensive algorithm benchmark results.
This script creates detailed visualizations comparing algorithm performance
across different dimensions, sizes, and distributions.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os
import numpy as np
import glob
from matplotlib.ticker import ScalarFormatter
import matplotlib.gridspec as gridspec

# Set style
plt.style.use('seaborn-v0_8-whitegrid')
sns.set_palette("colorblind")

# Constants
IMAGES_DIR = "benchmark_charts"
DPI = 300
SMALL_SIZE = 10
MEDIUM_SIZE = 12
BIGGER_SIZE = 14
TITLE_SIZE = 16

# Set font sizes
plt.rc('font', size=SMALL_SIZE)
plt.rc('axes', titlesize=BIGGER_SIZE)
plt.rc('axes', labelsize=MEDIUM_SIZE)
plt.rc('xtick', labelsize=SMALL_SIZE)
plt.rc('ytick', labelsize=SMALL_SIZE)
plt.rc('legend', fontsize=SMALL_SIZE)
plt.rc('figure', titlesize=TITLE_SIZE)

def ensure_dir(directory):
    """Ensure a directory exists; create it if it doesn't."""
    if not os.path.exists(directory):
        os.makedirs(directory)

def find_latest_benchmark_file():
    """Find the most recent benchmark CSV file."""
    benchmark_files = glob.glob("/Users/dylancanning/IdeaProjects/Practica_Cluster_De_Punts/benchmark_comprehensive_20250415_175837.csv")
    if not benchmark_files:
        return "/Users/dylancanning/IdeaProjects/Practica_Cluster_De_Punts/benchmark_comprehensive_20250415_175837.csv"  # Default fallback
    return max(benchmark_files, key=os.path.getctime)

def load_and_prepare_data(file_path=None):
    """Load and prepare the benchmark data."""
    if file_path is None:
        file_path = find_latest_benchmark_file()
    
    print(f"Loading benchmark data from: {file_path}")
    df = pd.read_csv(file_path)
    
    # Ensure numeric columns
    df['Size'] = pd.to_numeric(df['Size'])
    df['ExecutionTime(ms)'] = pd.to_numeric(df['ExecutionTime(ms)'])
    df['MemoryUsage(MB)'] = pd.to_numeric(df['MemoryUsage(MB)'])
    
    # Create algorithm group column (Closest Pair or Diameter)
    df['AlgorithmGroup'] = df['Algorithm'].apply(
        lambda x: 'Closest Pair' if 'CLOSEST_PAIR' in x else 'Diameter'
    )
    
    # Create algorithm type column (without dimension)
    df['AlgorithmType'] = df['Algorithm'].apply(
        lambda x: x.split('_')[0] if x.split('_')[0] == 'DIAMETER' else '_'.join(x.split('_')[0:2])
    )
    
    # Create algorithm variant column
    df['AlgorithmVariant'] = df['Algorithm'].apply(
        lambda x: x.split('_')[-1] if x.split('_')[0] == 'DIAMETER' else x.split('_')[-1]
    )
    
    # Add logarithmic execution time for better visualization
    df['LogExecutionTime'] = np.log10(df['ExecutionTime(ms)'])
    
    # Add an algorithm friendly name for better labels
    algorithm_names = {
        'CLOSEST_PAIR_NAIVE': 'Naive O(n²)',
        'CLOSEST_PAIR_EFFICIENT': 'Divide & Conquer',
        'CLOSEST_PAIR_KDTREE': 'KD-Tree',
        'CLOSEST_PAIR_ADAPTIVE': 'Adaptive',
        'DIAMETER_NAIVE': 'Naive O(n²)',
        'DIAMETER_CONCURRENT': 'Concurrent',
        'DIAMETER_QUICKHULL': 'QuickHull'
    }
    df['AlgorithmName'] = df['Algorithm'].apply(lambda x: algorithm_names.get(x, x))
    
    return df

def plot_time_complexity_by_dimension(df):
    """Plot time complexity (execution time vs size) for each algorithm, separated by dimension."""
    # Create separate plots for 2D and 3D
    for dimension in ['TWO_D', 'THREE_D']:
        dim_data = df[df['Dimension'] == dimension]
        dim_label = '2D' if dimension == 'TWO_D' else '3D'
        
        # Create separate plots for Closest Pair and Diameter
        for algo_group in ['Closest Pair', 'Diameter']:
            group_data = dim_data[dim_data['AlgorithmGroup'] == algo_group]
            
            # Skip if no data for this group
            if len(group_data) == 0:
                continue
            
            # Calculate average times for each algorithm/size combination
            plot_data = group_data.groupby(['Algorithm', 'Size'])['ExecutionTime(ms)'].mean().reset_index()
            
            # Create figure with two side-by-side subplots: Linear and Log scale
            fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 7))
            
            # Get unique algorithms for this group
            algorithms = plot_data['Algorithm'].unique()
            
            # Plot linear scale
            for algo in algorithms:
                algo_data = plot_data[plot_data['Algorithm'] == algo]
                algo_name = algorithm_names.get(algo, algo)
                ax1.plot(algo_data['Size'], algo_data['ExecutionTime(ms)'], 
                         marker='o', linewidth=2, label=algo_name)
            
            ax1.set_title(f'{algo_group} Algorithms - {dim_label} (Linear Scale)')
            ax1.set_xlabel('Number of Points (n)')
            ax1.set_ylabel('Execution Time (ms)')
            ax1.legend()
            ax1.grid(True)
            
            # Plot log scale
            for algo in algorithms:
                algo_data = plot_data[plot_data['Algorithm'] == algo]
                algo_name = algorithm_names.get(algo, algo)
                ax2.loglog(algo_data['Size'], algo_data['ExecutionTime(ms)'], 
                           marker='o', linewidth=2, label=algo_name)
            
            # Add reference lines for common complexity classes
            x_range = np.linspace(min(plot_data['Size']), max(plot_data['Size']), 100)
            scale_factor = plot_data['ExecutionTime(ms)'].max() / (max(plot_data['Size'])**2) * 0.1
            
            ax2.loglog(x_range, scale_factor * x_range, 'k--', alpha=0.5, label='O(n)')
            ax2.loglog(x_range, scale_factor * x_range * np.log(x_range), 'k-.', alpha=0.5, label='O(n log n)')
            ax2.loglog(x_range, scale_factor * x_range**2, 'k:', alpha=0.5, label='O(n²)')
            
            ax2.set_title(f'{algo_group} Algorithms - {dim_label} (Log-Log Scale)')
            ax2.set_xlabel('Number of Points (n)')
            ax2.set_ylabel('Execution Time (ms)')
            ax2.legend()
            ax2.grid(True)
            
            plt.tight_layout()
            
            # Save figure
            ensure_dir(IMAGES_DIR)
            fig.savefig(f"{IMAGES_DIR}/{dim_label}_{algo_group.replace(' ', '')}_complexity.png", dpi=DPI)
            plt.close(fig)

def plot_distribution_comparison(df):
    """Plot comparison of algorithm performance across different distributions."""
    # Focus on the largest size for a fair comparison
    max_size = df['Size'].max()
    largest_data = df[df['Size'] == max_size]
    
    # Calculate mean execution time for each algorithm, dimension, and distribution
    grouped = largest_data.groupby(['Algorithm', 'Dimension', 'Distribution'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Create separate plots for 2D and 3D
    for dimension in ['TWO_D', 'THREE_D']:
        dim_data = grouped[grouped['Dimension'] == dimension]
        dim_label = '2D' if dimension == 'TWO_D' else '3D'
        
        # Create separate plots for Closest Pair and Diameter
        for algo_group in ['Closest Pair', 'Diameter']:
            # Filter by algorithm group
            group_data = dim_data[dim_data['Algorithm'].str.contains(
                'CLOSEST_PAIR' if algo_group == 'Closest Pair' else 'DIAMETER')]
            
            # Skip if no data for this group
            if len(group_data) == 0:
                continue
            
            # Pivot the data for plotting
            pivot_data = group_data.pivot(index='Algorithm', columns='Distribution', values='ExecutionTime(ms)')
            
            # Create the bar plot
            fig, ax = plt.subplots(figsize=(12, 8))
            pivot_data.plot(kind='bar', ax=ax)
            
            plt.title(f'{algo_group} Algorithms - {dim_label} Distribution Comparison (n={max_size})')
            plt.xlabel('Algorithm')
            plt.ylabel('Execution Time (ms)')
            plt.xticks(rotation=45)
            plt.tight_layout()
            
            # Save figure
            ensure_dir(IMAGES_DIR)
            fig.savefig(f"{IMAGES_DIR}/{dim_label}_{algo_group.replace(' ', '')}_distributions.png", dpi=DPI)
            plt.close(fig)

def plot_memory_usage(df):
    """Plot memory usage for different algorithms and sizes."""
    # Calculate mean memory usage for each algorithm, dimension, and size
    grouped = df.groupby(['Algorithm', 'Dimension', 'Size'])['MemoryUsage(MB)'].mean().reset_index()
    
    # Create separate plots for 2D and 3D
    for dimension in ['TWO_D', 'THREE_D']:
        dim_data = grouped[grouped['Dimension'] == dimension]
        dim_label = '2D' if dimension == 'TWO_D' else '3D'
        
        # Create separate plots for Closest Pair and Diameter
        for algo_group in ['Closest Pair', 'Diameter']:
            # Filter by algorithm group
            group_data = dim_data[dim_data['Algorithm'].str.contains(
                'CLOSEST_PAIR' if algo_group == 'Closest Pair' else 'DIAMETER')]
            
            # Skip if no data for this group
            if len(group_data) == 0:
                continue
            
            # Create the plot
            fig, ax = plt.subplots(figsize=(12, 8))
            
            # Get unique algorithms for this group
            algorithms = group_data['Algorithm'].unique()
            
            # Plot memory usage vs size for each algorithm
            for algo in algorithms:
                algo_data = group_data[group_data['Algorithm'] == algo]
                algo_name = algorithm_names.get(algo, algo)
                ax.plot(algo_data['Size'], algo_data['MemoryUsage(MB)'], 
                        marker='o', linewidth=2, label=algo_name)
            
            plt.title(f'{algo_group} Algorithms - {dim_label} Memory Usage')
            plt.xlabel('Number of Points (n)')
            plt.ylabel('Memory Usage (MB)')
            plt.legend()
            plt.grid(True)
            plt.tight_layout()
            
            # Save figure
            ensure_dir(IMAGES_DIR)
            fig.savefig(f"{IMAGES_DIR}/{dim_label}_{algo_group.replace(' ', '')}_memory.png", dpi=DPI)
            plt.close(fig)

def plot_dimension_comparison(df):
    """Compare algorithm performance across dimensions (2D vs 3D)."""
    # Calculate mean execution time for each algorithm, dimension, and size
    grouped = df.groupby(['Algorithm', 'AlgorithmName', 'Dimension', 'Size'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Create separate plots for each algorithm
    for algo in grouped['Algorithm'].unique():
        algo_data = grouped[grouped['Algorithm'] == algo]
        algo_name = algo_data['AlgorithmName'].iloc[0]
        
        # Create the plot
        fig, ax = plt.subplots(figsize=(12, 8))
        
        # Plot 2D and 3D performance
        for dimension in ['TWO_D', 'THREE_D']:
            dim_data = algo_data[algo_data['Dimension'] == dimension]
            dim_label = '2D' if dimension == 'TWO_D' else '3D'
            
            if len(dim_data) > 0:
                ax.plot(dim_data['Size'], dim_data['ExecutionTime(ms)'], 
                        marker='o', linewidth=2, label=dim_label)
        
        plt.title(f'{algo_name} - 2D vs 3D Performance Comparison')
        plt.xlabel('Number of Points (n)')
        plt.ylabel('Execution Time (ms)')
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        
        # Save figure
        ensure_dir(IMAGES_DIR)
        fig.savefig(f"{IMAGES_DIR}/{algo}_dimension_comparison.png", dpi=DPI)
        plt.close(fig)

def plot_performance_heatmap(df):
    """Create heatmaps showing relative performance of algorithms."""
    # Focus on a specific distribution (e.g., UNIFORM)
    uniform_data = df[df['Distribution'] == 'UNIFORM']
    
    # Calculate mean execution time for each algorithm, dimension, and size
    grouped = uniform_data.groupby(['Algorithm', 'AlgorithmName', 'Dimension', 'Size'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Create separate heatmaps for 2D and 3D
    for dimension in ['TWO_D', 'THREE_D']:
        dim_data = grouped[grouped['Dimension'] == dimension]
        dim_label = '2D' if dimension == 'TWO_D' else '3D'
        
        # Create separate heatmaps for Closest Pair and Diameter
        for algo_group in ['Closest Pair', 'Diameter']:
            # Filter by algorithm group
            group_data = dim_data[dim_data['Algorithm'].str.contains(
                'CLOSEST_PAIR' if algo_group == 'Closest Pair' else 'DIAMETER')]
            
            # Skip if no data for this group
            if len(group_data) == 0:
                continue
            
            # Pivot data for heatmap
            pivot_data = group_data.pivot(index='Size', columns='AlgorithmName', values='ExecutionTime(ms)')
            
            # Create the heatmap
            fig, ax = plt.subplots(figsize=(12, 10))
            sns.heatmap(pivot_data, annot=True, fmt='.1f', cmap='viridis', ax=ax)
            
            plt.title(f'{algo_group} Algorithms - {dim_label} Performance Heatmap (UNIFORM)')
            plt.ylabel('Number of Points (n)')
            plt.xlabel('Algorithm')
            plt.tight_layout()
            
            # Save figure
            ensure_dir(IMAGES_DIR)
            fig.savefig(f"{IMAGES_DIR}/{dim_label}_{algo_group.replace(' ', '')}_heatmap.png", dpi=DPI)
            plt.close(fig)

def plot_relative_speedup(df):
    """Plot relative speedup of optimized algorithms compared to naive implementation."""
    # Calculate mean execution time for each algorithm, dimension, and size
    grouped = df.groupby(['Algorithm', 'AlgorithmName', 'Dimension', 'Size', 'Distribution'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Create separate plots for 2D and 3D
    for dimension in ['TWO_D', 'THREE_D']:
        dim_data = grouped[grouped['Dimension'] == dimension]
        dim_label = '2D' if dimension == 'TWO_D' else '3D'
        
        # Create separate plots for Closest Pair and Diameter
        for algo_group in ['Closest Pair', 'Diameter']:
            # Filter by algorithm group
            if algo_group == 'Closest Pair':
                group_data = dim_data[dim_data['Algorithm'].str.contains('CLOSEST_PAIR')]
                naive_algo = 'CLOSEST_PAIR_NAIVE'
            else:
                group_data = dim_data[dim_data['Algorithm'].str.contains('DIAMETER')]
                naive_algo = 'DIAMETER_NAIVE'
            
            # Skip if no data for this group or if naive algorithm is missing
            if len(group_data) == 0 or not any(group_data['Algorithm'] == naive_algo):
                continue
            
            # Process each distribution
            for dist in group_data['Distribution'].unique():
                dist_data = group_data[group_data['Distribution'] == dist]
                
                fig, ax = plt.subplots(figsize=(12, 8))
                
                # Get naive algorithm times for reference
                naive_data = dist_data[dist_data['Algorithm'] == naive_algo]
                
                # Calculate speedup for each algorithm relative to naive
                for algo in dist_data['Algorithm'].unique():
                    if algo == naive_algo:
                        continue
                    
                    algo_data = dist_data[dist_data['Algorithm'] == algo]
                    
                    # Merge with naive data to calculate speedup
                    merged_data = pd.merge(
                        algo_data, 
                        naive_data[['Size', 'ExecutionTime(ms)']],
                        on='Size',
                        suffixes=('', '_naive')
                    )
                    
                    # Calculate speedup
                    merged_data['Speedup'] = merged_data['ExecutionTime(ms)_naive'] / merged_data['ExecutionTime(ms)']
                    
                    # Plot speedup
                    ax.plot(merged_data['Size'], merged_data['Speedup'], 
                            marker='o', linewidth=2, label=algorithm_names.get(algo, algo))
                
                plt.title(f'{algo_group} Algorithms - {dim_label} Speedup vs Naive ({dist})')
                plt.xlabel('Number of Points (n)')
                plt.ylabel('Speedup Factor (higher is better)')
                plt.legend()
                plt.grid(True)
                
                # Add horizontal line at y=1 (no speedup)
                plt.axhline(y=1, color='r', linestyle='--', alpha=0.5)
                
                plt.tight_layout()
                
                # Save figure
                ensure_dir(IMAGES_DIR)
                fig.savefig(f"{IMAGES_DIR}/{dim_label}_{algo_group.replace(' ', '')}_{dist}_speedup.png", dpi=DPI)
                plt.close(fig)

def create_unified_dashboard(df):
    """Create unified dashboard with key performance metrics."""
    # Create a single large figure with multiple subplots
    fig = plt.figure(figsize=(24, 20))
    gs = gridspec.GridSpec(4, 4, figure=fig)
    
    # 1. Time complexity (2D Closest Pair)
    ax1 = fig.add_subplot(gs[0, :2])
    plot_time_complexity_dashboard(df, 'TWO_D', 'Closest Pair', ax1)
    
    # 2. Time complexity (3D Closest Pair)
    ax2 = fig.add_subplot(gs[0, 2:])
    plot_time_complexity_dashboard(df, 'THREE_D', 'Closest Pair', ax2)
    
    # 3. Time complexity (2D Diameter)
    ax3 = fig.add_subplot(gs[1, :2])
    plot_time_complexity_dashboard(df, 'TWO_D', 'Diameter', ax3)
    
    # 4. Time complexity (3D Diameter)
    ax4 = fig.add_subplot(gs[1, 2:])
    plot_time_complexity_dashboard(df, 'THREE_D', 'Diameter', ax4)
    
    # 5. Distribution comparison (2D Closest Pair)
    ax5 = fig.add_subplot(gs[2, :2])
    plot_distribution_dashboard(df, 'TWO_D', 'Closest Pair', ax5)
    
    # 6. Distribution comparison (3D Closest Pair)
    ax6 = fig.add_subplot(gs[2, 2:])
    plot_distribution_dashboard(df, 'THREE_D', 'Closest Pair', ax6)
    
    # 7. Speedup comparison (2D)
    ax7 = fig.add_subplot(gs[3, :2])
    plot_speedup_dashboard(df, 'TWO_D', ax7)
    
    # 8. Speedup comparison (3D)
    ax8 = fig.add_subplot(gs[3, 2:])
    plot_speedup_dashboard(df, 'THREE_D', ax8)
    
    plt.suptitle('Point Cloud Algorithm Performance Dashboard', fontsize=24)
    plt.tight_layout(rect=[0, 0, 1, 0.97])
    
    # Save figure
    ensure_dir(IMAGES_DIR)
    fig.savefig(f"{IMAGES_DIR}/performance_dashboard.png", dpi=DPI)
    plt.close(fig)

def plot_time_complexity_dashboard(df, dimension, algo_group, ax):
    """Helper function to plot time complexity for the dashboard."""
    dim_data = df[df['Dimension'] == dimension]
    dim_label = '2D' if dimension == 'TWO_D' else '3D'
    
    # Filter by algorithm group
    group_data = dim_data[dim_data['Algorithm'].str.contains(
        'CLOSEST_PAIR' if algo_group == 'Closest Pair' else 'DIAMETER')]
    
    # Calculate average times for each algorithm/size combination
    plot_data = group_data.groupby(['Algorithm', 'Size'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Get unique algorithms for this group
    algorithms = plot_data['Algorithm'].unique()
    
    # Plot log-log scale
    for algo in algorithms:
        algo_data = plot_data[plot_data['Algorithm'] == algo]
        algo_name = algorithm_names.get(algo, algo)
        ax.loglog(algo_data['Size'], algo_data['ExecutionTime(ms)'], 
                   marker='o', linewidth=2, label=algo_name)
    
    # Add reference lines for common complexity classes
    x_range = np.linspace(min(plot_data['Size']), max(plot_data['Size']), 100)
    scale_factor = plot_data['ExecutionTime(ms)'].max() / (max(plot_data['Size'])**2) * 0.1
    
    ax.loglog(x_range, scale_factor * x_range, 'k--', alpha=0.5, label='O(n)')
    ax.loglog(x_range, scale_factor * x_range * np.log(x_range), 'k-.', alpha=0.5, label='O(n log n)')
    ax.loglog(x_range, scale_factor * x_range**2, 'k:', alpha=0.5, label='O(n²)')
    
    ax.set_title(f'{algo_group} Algorithms - {dim_label} (Log-Log Scale)')
    ax.set_xlabel('Number of Points (n)')
    ax.set_ylabel('Execution Time (ms)')
    ax.legend()
    ax.grid(True)

def plot_distribution_dashboard(df, dimension, algo_group, ax):
    """Helper function to plot distribution comparison for the dashboard."""
    # Focus on the largest size for a fair comparison
    max_size = df['Size'].max()
    largest_data = df[df['Size'] == max_size]
    
    dim_data = largest_data[largest_data['Dimension'] == dimension]
    dim_label = '2D' if dimension == 'TWO_D' else '3D'
    
    # Calculate mean execution time for each algorithm and distribution
    group_data = dim_data[dim_data['Algorithm'].str.contains(
        'CLOSEST_PAIR' if algo_group == 'Closest Pair' else 'DIAMETER')]
    
    grouped = group_data.groupby(['Algorithm', 'Distribution'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Pivot the data for plotting
    pivot_data = grouped.pivot(index='Algorithm', columns='Distribution', values='ExecutionTime(ms)')
    
    # Create the bar plot
    pivot_data.plot(kind='bar', ax=ax)
    
    ax.set_title(f'{algo_group} Algorithms - {dim_label} Distribution Comparison (n={max_size})')
    ax.set_xlabel('Algorithm')
    ax.set_ylabel('Execution Time (ms)')
    ax.tick_params(axis='x', rotation=45)
    ax.legend(title='Distribution')

def plot_speedup_dashboard(df, dimension, ax):
    """Helper function to plot speedup comparison for the dashboard."""
    dim_data = df[df['Dimension'] == dimension]
    dim_label = '2D' if dimension == 'TWO_D' else '3D'
    
    # Use only UNIFORM distribution for clarity
    uniform_data = dim_data[dim_data['Distribution'] == 'UNIFORM']
    
    # Calculate mean execution time for each algorithm and size
    grouped = uniform_data.groupby(['Algorithm', 'AlgorithmName', 'Size'])['ExecutionTime(ms)'].mean().reset_index()
    
    # Split into Closest Pair and Diameter
    cp_data = grouped[grouped['Algorithm'].str.contains('CLOSEST_PAIR')]
    dm_data = grouped[grouped['Algorithm'].str.contains('DIAMETER')]
    
    # Get naive algorithm times
    cp_naive = cp_data[cp_data['Algorithm'] == 'CLOSEST_PAIR_NAIVE']
    dm_naive = dm_data[dm_data['Algorithm'] == 'DIAMETER_NAIVE']
    
    # Plot speedup for Closest Pair algorithms
    for algo in cp_data['Algorithm'].unique():
        if algo == 'CLOSEST_PAIR_NAIVE':
            continue
        
        algo_data = cp_data[cp_data['Algorithm'] == algo]
        
        # Merge with naive data
        merged_data = pd.merge(
            algo_data, 
            cp_naive[['Size', 'ExecutionTime(ms)']],
            on='Size',
            suffixes=('', '_naive')
        )
        
        # Calculate speedup
        merged_data['Speedup'] = merged_data['ExecutionTime(ms)_naive'] / merged_data['ExecutionTime(ms)']
        
        # Plot with dashed line
        ax.plot(merged_data['Size'], merged_data['Speedup'], 
                marker='o', linewidth=2, linestyle='--',
                label=f"CP: {algorithm_names.get(algo, algo)}")
    
    # Plot speedup for Diameter algorithms
    for algo in dm_data['Algorithm'].unique():
        if algo == 'DIAMETER_NAIVE':
            continue
        
        algo_data = dm_data[dm_data['Algorithm'] == algo]
        
        # Merge with naive data
        merged_data = pd.merge(
            algo_data, 
            dm_naive[['Size', 'ExecutionTime(ms)']],
            on='Size',
            suffixes=('', '_naive')
        )
        
        # Calculate speedup
        merged_data['Speedup'] = merged_data['ExecutionTime(ms)_naive'] / merged_data['ExecutionTime(ms)']
        
        # Plot with solid line
        ax.plot(merged_data['Size'], merged_data['Speedup'], 
                marker='o', linewidth=2,
                label=f"DM: {algorithm_names.get(algo, algo)}")
    
    ax.set_title(f'{dim_label} Algorithm Speedup vs Naive (UNIFORM)')
    ax.set_xlabel('Number of Points (n)')
    ax.set_ylabel('Speedup Factor (higher is better)')
    ax.legend()
    ax.grid(True)
    
    # Add horizontal line at y=1 (no speedup)
    ax.axhline(y=1, color='r', linestyle='--', alpha=0.5)

# Define algorithm friendly names globally
algorithm_names = {
    'CLOSEST_PAIR_NAIVE': 'Naive O(n²)',
    'CLOSEST_PAIR_EFFICIENT': 'Divide & Conquer',
    'CLOSEST_PAIR_KDTREE': 'KD-Tree',
    'CLOSEST_PAIR_ADAPTIVE': 'Adaptive',
    'DIAMETER_NAIVE': 'Naive O(n²)',
    'DIAMETER_CONCURRENT': 'Concurrent',
    'DIAMETER_QUICKHULL': 'QuickHull'
}

def main():
    """Main function to generate all plots."""
    try:
        # Find the most recent benchmark file
        csv_file = find_latest_benchmark_file()
        
        # Load and prepare the data
        df = load_and_prepare_data(csv_file)
        
        print(f"Generating plots for {len(df)} benchmark records...")
        
        # Generate all plots
        plot_time_complexity_by_dimension(df)
        plot_distribution_comparison(df)
        plot_memory_usage(df)
        plot_dimension_comparison(df)
        plot_performance_heatmap(df)
        plot_relative_speedup(df)
        create_unified_dashboard(df)
        
        print(f"Plotting complete. Check the '{IMAGES_DIR}' directory for the generated visualizations.")
        
    except Exception as e:
        print(f"Error during plotting: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()