import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from scipy.interpolate import griddata


# 🔹 Step 1: Read the text file and extract metrics
def read_metrics_from_file(filename):
    data = {
        "Quantum1": [],
        "Quantum2": [],
        "Throughput": [],
        "WaitingTime": [],
        "TurnaroundTime": [],
        "ResponseTime": []
    }

    with open(filename, "r") as file:
        lines = [line.strip() for line in file.readlines()]  # Remove whitespace

    i = 0
    while i < len(lines):
        if lines[i].startswith("--------- System Metrics ---------"):  # Skip block headers
            i += 1  # Move to the next line
            continue

        if lines[i].startswith("file:"):  # Skip file path line
            i += 1
            continue

        if i + 5 >= len(lines):  # Ensure enough lines exist to extract a full block
            print(f"Skipping incomplete metric block starting at line {i}")
            break

        try:
            # Extracting values safely
            throughput = float(lines[i].split(":")[1].strip()) if ":" in lines[i] else None
            waiting_time = float(lines[i + 1].split(":")[1].strip()) if ":" in lines[i + 1] else None
            turnaround_time = float(lines[i + 2].split(":")[1].strip()) if ":" in lines[i + 2] else None
            response_time = float(lines[i + 3].split(":")[1].strip()) if ":" in lines[i + 3] else None
            q1 = float(lines[i + 4].split(":")[1].strip()) if ":" in lines[i + 4] else None
            q2 = float(lines[i + 5].split(":")[1].strip()) if ":" in lines[i + 5] else None

            # Ensure all values are valid
            if None in [throughput, waiting_time, turnaround_time, response_time, q1, q2]:
                print(f"Skipping invalid metric block at line {i}: Missing values")
            else:
                data["Quantum1"].append(q1)
                data["Quantum2"].append(q2)
                data["Throughput"].append(throughput)
                data["WaitingTime"].append(waiting_time)
                data["TurnaroundTime"].append(turnaround_time)
                data["ResponseTime"].append(response_time)

        except (ValueError, IndexError) as e:
            print(f"Skipping malformed metric block at line {i}: {e}")

        i += 6  # Move to the next block

    return pd.DataFrame(data)


# 🔹 Step 2: Plot 3D Surface Plot and Save as PNG
def plot_3d_surface(df, metric, output_dir="plots"):
    import os
    os.makedirs(output_dir, exist_ok=True)  # Ensure the output directory exists

    if df.empty:
        print(f"No valid data available for {metric}. Skipping plot.")
        return

    fig = plt.figure(figsize=(8, 6))
    ax = fig.add_subplot(111, projection="3d")

    quantum1 = np.array(df["Quantum1"])
    quantum2 = np.array(df["Quantum2"])
    metric_values = np.array(df[metric])

    # Generate grid for smoother interpolation
    grid_x, grid_y = np.meshgrid(
        np.linspace(min(quantum1), max(quantum1), 30),
        np.linspace(min(quantum2), max(quantum2), 30)
    )

    grid_z = griddata((quantum1, quantum2), metric_values, (grid_x, grid_y), method="cubic")

    # Plot surface
    surf = ax.plot_surface(grid_x, grid_y, grid_z, cmap="viridis", edgecolor="black", alpha=0.7)

    # Scatter raw data points (red dots)
    ax.scatter(quantum1, quantum2, metric_values, color="red", s=20)

    # Labels
    ax.set_xlabel("Quantum 1")
    ax.set_ylabel("Quantum 2")
    ax.set_zlabel(metric)
    ax.set_title(f"3D Surface Plot: {metric}")

    # Save plot as PNG
    filepath = os.path.join(output_dir, f"{metric}.png")
    plt.savefig(filepath, dpi=72, bbox_inches="tight")  # High-resolution PNG
    print(f"Saved {metric} plot as {filepath}")

    plt.close(fig)  # Close the figure to free memory


# 🔹 Step 3: Read file & generate PNGs
filename = "metrics_output.txt"  # Replace with actual file path
df = read_metrics_from_file(filename)

# Generate and save plots
for metric in ["Throughput", "WaitingTime", "TurnaroundTime", "ResponseTime"]:
    plot_3d_surface(df, metric)
