/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.indexer.parser;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;

import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

public class ResolveSolrField extends SolrField {

    private static final String RESOLVE_PATH = "/cn/v1/resolve/";

    public ResolveSolrField(String name) {
        setName(name);
    }

    @Override
    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {
        List<SolrElementField> fields = new ArrayList<SolrElementField>();
        String hostname = InetAddress.getLocalHost().getCanonicalHostName();
        String pid = identifier;
        pid = URLEncoder.encode(pid, "UTF-8");
        fields.add(new SolrElementField(getName(), "https://" + hostname + RESOLVE_PATH + pid));
        return fields;
    }

    @Override
    public void initExpression(XPath xpathObject) {
        // this solr field does not make use of an xpath expression so override
        // with empty behavior.
    }
}