/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.platform.util;

import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.actions.ServerInstall;
import net.minecraftforge.installer.json.Install;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Predicate;

public class ForgeInstalls {
    public static class Client extends ClientInstall {

        private final Path libraries;
        private final File client;

        public Client(Install profile, ProgressCallback monitor, Path libraries, File client) {
            super(profile, monitor);
            this.libraries = libraries;
            this.client = client;
        }

        @Override
        public boolean run(File target, Predicate<String> optionals) {
            return this.processors.process(this.libraries.toFile(), this.client);
        }
    }

    public static class Server extends ServerInstall {

        private final Path libraries;
        private final File client;

        public Server(Install profile, ProgressCallback monitor, Path libraries, File client) {
            super(profile, monitor);
            this.libraries = libraries;
            this.client = client;
        }

        @Override
        public boolean run(File target, Predicate<String> optionals) {
            return this.processors.process(this.libraries.toFile(), this.client);
        }
    }
}
