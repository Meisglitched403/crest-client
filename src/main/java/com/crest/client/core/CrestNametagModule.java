package com.crest.client.core;

public class CrestNametagModule implements CrestModule {
    @Override
    public String getId() { return "crest_nametag"; }
    @Override
    public String getName() { return "Crest Nametag"; }
    @Override
    public String getDescription() { return "Shows the crest-client logo next to player nametags"; }
    @Override
    public String getCategory() { return "Visual"; }
}
