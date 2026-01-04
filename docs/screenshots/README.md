# Screenshots Directory

This directory contains screenshots and images for the project documentation.

## Structure

```
docs/screenshots/
├── README.md                    # This file
├── swagger/                     # Swagger UI screenshots
│   ├── swagger-ui_home.png
│   └── swagger-ui_set_token.png
├── grafana/                     # Grafana dashboard screenshots
│   ├── grafana_add_data_sources.png
│   ├── grafana_import_dashboard.png
│   ├── grafana_import_dashboard_2.png
│   └── grafana_view_example_dashboard.png
└── kibana/                      # Kibana log screenshots
    ├── kibana_first_screen.png
    ├── kibana_discover_logs.png
    └── kibana_create_view.png
```

## Adding Screenshots

1. Take screenshots and save them in the appropriate subdirectory
2. Use descriptive filenames (kebab-case)
3. Update `README.md` in the root directory to reference the screenshots
4. Keep file sizes reasonable (< 500KB recommended)

## Screenshot Guidelines

- **Resolution**: 1920x1080 or higher
- **Format**: PNG or JPG
- **Naming**: Use descriptive names (e.g., `swagger-ui-authentication-endpoints.png`)
- **Annotations**: Add arrows or highlights if needed to point out important features

