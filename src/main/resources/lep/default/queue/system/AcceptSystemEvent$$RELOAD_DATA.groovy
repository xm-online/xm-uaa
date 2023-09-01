import com.icthh.xm.uaa.lep.LepContext
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())
LepContext context = lepContext
def data = context.inArgs.event.data
log.info("#### =============RELOAD DATA FROM UAA NOT IMPLEMNETED===========>>>>>>>>>> {}", data)
