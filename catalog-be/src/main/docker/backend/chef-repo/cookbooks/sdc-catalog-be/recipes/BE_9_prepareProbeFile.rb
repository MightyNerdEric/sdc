if node[:disableHttp]
  protocol = "https"
  port = "#{node['BE'][:https_port]}"
else
  protocol = "http"
  port = "#{node['BE'][:http_port]}"
end

template "#{ENV['JETTY_BASE']}/ready-probe.sh" do
  source "ready-probe.sh.erb"
  sensitive true
  mode 0755
  variables({
    :protocol => protocol,
    :port => port
  })
end
