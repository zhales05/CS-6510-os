import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from mpl_toolkits.mplot3d import Axes3D
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
        lines = file.readlines()

        for i in range(0, len(lines), 7):  # Each metric block is 7 lines long
            throughput = float(lines[i + 1].split(":")[1].strip())
            waiting_time = float(lines[i + 2].split(":")[1].strip())
            turnaround_time = float(lines[i + 3].split(":")[1].strip())
            response_time = float(lines[i + 4].split(":")[1].strip())
            q1 = float(lines[i + 5].split(":")[1].strip())
            q2 = float(lines[i + 6].split(":")[1].strip())

            data["Quantum1"].append(q1)
            data["Quantum2"].append(q2)
            data["Throughput"].append(throughput)
            data["WaitingTime"].append(waiting_time)
            data["TurnaroundTime"].append(turnaround_time)
            data["ResponseTime"].append(response_time)

    return pd.DataFrame(data)

# 🔹 Step 2: Plot 3D Surface Plot
def plot_3d_surface(df, metric):
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

    grid_z = griddata((quantum1, quantum2), metric_values, (grid_x, grid_y), method="linear")

    # Plot surface
    surf = ax.plot_surface(grid_x, grid_y, grid_z, cmap="viridis", edgecolor="black", alpha=0.7)

    # Scatter raw data points (red dots)
    ax.scatter(quantum1, quantum2, metric_values, color="red", s=20)

    # Labels
    ax.set_xlabel("Quantum 1")
    ax.set_ylabel("Quantum 2")
    ax.set_zlabel(metric)
    ax.set_title(f"3D Surface Plot: {metric}")

    plt.show()

# 🔹 Step 3: Read file & plot all metrics
filename = "metrics_output.txt"  # Replace with the actual file path
df = read_metrics_from_file(filename)

# Plot each metric as a separate 3D surface
plot_3d_surface(df, "Throughput")
plot_3d_surface(df, "WaitingTime")
plot_3d_surface(df, "TurnaroundTime")
plot_3d_surface(df, "ResponseTime")
