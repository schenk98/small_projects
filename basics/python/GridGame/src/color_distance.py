"""Tk color comparison for minimum perceptual separation between players."""

MIN_CHANNEL_DELTA = 51
"""Two colors are too similar if every channel differs by *strictly less* than this (0–255 scale)."""


def tk_color_to_rgb8(widget, color: str) -> tuple[int, int, int]:
    """Map any Tk color string to approximate 8-bit RGB using the widget's display."""
    r16, g16, b16 = widget.winfo_rgb(color)
    return (
        min(255, round(r16 * 255 / 65535)),
        min(255, round(g16 * 255 / 65535)),
        min(255, round(b16 * 255 / 65535)),
    )


def max_channel_delta(rgb_a: tuple[int, int, int], rgb_b: tuple[int, int, int]) -> int:
    return max(abs(rgb_a[i] - rgb_b[i]) for i in range(3))


def colors_too_similar(
    widget,
    color_a: str,
    color_b: str,
    min_channel_delta: int = MIN_CHANNEL_DELTA,
) -> bool:
    """
    True if the two Tk colors are too close in RGB (Chebyshev on 8-bit channels).

    Distinct enough requires max(|dr|,|dg|,|db|) >= min_channel_delta.
    """
    a = tk_color_to_rgb8(widget, color_a)
    b = tk_color_to_rgb8(widget, color_b)
    return max_channel_delta(a, b) < min_channel_delta
