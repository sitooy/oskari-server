UPDATE portti_view_bundle_seq
	SET config=
	'{
    "supportedProjections":
        [
            "EPSG:3067",
            "EPSG:4326",
            "EPSG:2393",
            "EPSG:4258",
            "EPSG:3873",
            "EPSG:3874",
            "EPSG:3875",
            "EPSG:3876",
            "EPSG:3877",
            "EPSG:3878",
            "EPSG:3879",
            "EPSG:3880",
            "EPSG:3881",
            "EPSG:3882",
            "EPSG:3883",
            "EPSG:3884",
            "EPSG:3885"
        ],
    "projectionShowFormat":
	    {
	    "decimals":3,
	    "format":"metric",
	    "EPSG:2393":{"decimals":0},
	    "EPSG:3067":{"decimals":0},
	    "EPSG:4326":{"decimals":3,"format":"degrees"},
	    "EPSG:4258":{"decimals":3,"format":"degrees"}
	    }
    }'
    WHERE bundleinstance = 'coordinatetool'